package org.unibl.etf.krz.evoting.ca;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public abstract class CA {

    protected final String name;
    protected final String keystorePath;
    protected final String crlPath;
    protected final String serialCounterPath;
    protected final String keystorePassword;

    protected KeyPair keyPair;
    protected X509Certificate caCertificate;
    protected X509CRL crl;

    private final AtomicLong serialCounter = new AtomicLong();

    protected static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    protected static final String KEY_ALGORITHM = "RSA";
    protected static final int KEY_SIZE = 2048;

    protected static final long END_ENTITY_VALIDITY_MS = 1L * 365 * 24 * 60 * 60 * 1000;
    protected static final long CA_CERT_VALIDITY_MS = 10L * 365 * 24 * 60 * 60 * 1000;

    protected CA(String name, String keystorePath, String crlPath, String serialCounterPath, String keystorePassword) {
        this.name = name;
        this.keystorePath = keystorePath;
        this.crlPath = crlPath;
        this.serialCounterPath = serialCounterPath;
        this.keystorePassword = keystorePassword;
    }

    protected abstract ExtensionsGenerator getExtensions() throws Exception;
    protected abstract void validateRequest(PKCS10CertificationRequest csr) throws Exception;

    public void loadFromKeystore() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
        try (FileInputStream is = new FileInputStream(keystorePath)) {
            ks.load(is, keystorePassword.toCharArray());
        }

        String alias = name.toLowerCase();

        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, keystorePassword.toCharArray());
        caCertificate = (X509Certificate) ks.getCertificate(alias);

        if (privateKey == null || caCertificate == null) {
            throw new KeyStoreException("Could not load key/certificate for alias: " + alias);
        }

        keyPair = new KeyPair(caCertificate.getPublicKey(), privateKey);

        File crlFile = new File(crlPath);
        if (crlFile.exists()) {
            loadCRL();
        } else {
            initEmptyCRL();
        }

        loadSerialCounter();
    }

    public void saveToKeystore() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
        ks.load(null, null);

        String alias = name.toLowerCase();
        ks.setKeyEntry(
                alias,
                keyPair.getPrivate(),
                keystorePassword.toCharArray(),
                new Certificate[]{ caCertificate }
        );

        try (FileOutputStream os = new FileOutputStream(keystorePath)) {
            ks.store(os, keystorePassword.toCharArray());
        }
    }

    public X509Certificate grantCertificate(PKCS10CertificationRequest csr, long validityMs) throws Exception {

        validateRequest(csr);

        if(!csr.isSignatureValid(
                new JcaContentVerifierProviderBuilder()
                        .setProvider("BC")
                        .build(csr.getSubjectPublicKeyInfo()))) {
            throw new SecurityException("CSR signature validation failed.");
        }

        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + validityMs);
        X500Name issuerName = new JcaX509CertificateHolder(caCertificate).getSubject();
        X500Name subjectName = csr.getSubject();
        BigInteger serial = getNextSerialNumber();
        incrementSerialNumber();

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                issuerName,
                serial,
                notBefore,
                notAfter,
                subjectName,
                csr.getSubjectPublicKeyInfo()
        );

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        certBuilder.addExtension(
                Extension.authorityKeyIdentifier,
                false,
                extUtils.createAuthorityKeyIdentifier(caCertificate)
        );

        certBuilder.addExtension(
                Extension.subjectKeyIdentifier,
                false,
                extUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo())
        );

        ExtensionsGenerator extGen = getExtensions();
        Extensions extensions = extGen.generate();
        for (ASN1ObjectIdentifier oid : extensions.getExtensionOIDs()) {
            Extension ext = extensions.getExtension(oid);
            certBuilder.addExtension(oid, ext.isCritical(), ext.getParsedValue());
        }

        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .setProvider("BC")
                .build(keyPair.getPrivate());

        X509CertificateHolder holder = certBuilder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(holder);
    }

    public void validateCertificate(X509Certificate cert) throws Exception {

        cert.checkValidity();

        X500Name certIssuer = new JcaX509CertificateHolder(cert).getIssuer();
        X500Name caSubject = new JcaX509CertificateHolder(caCertificate).getSubject();

        if (!certIssuer.equals(caSubject)) {
            throw new CertificateException("Certificate issuer does not match CA: " + certIssuer);
        }

        try {
            cert.verify(caCertificate.getPublicKey(), "BC");
        } catch (SignatureException e) {
            throw new CertificateException("Certificate signature verification failed.", e);
        }

        if (crl != null && crl.isRevoked(cert)) {
            throw new CertificateException("Certificate has been revoked. Serial: " + cert.getSerialNumber());
        }
    }

    public void revokeCertificate(X509Certificate cert) throws Exception {
        updateCRL(cert.getSerialNumber());
    }

    protected void updateCRL(BigInteger revokedSerial) throws Exception {
        Date now = new Date();
        Date nextUpdate = new Date(now.getTime() + 30L * 24 * 60 * 60 * 1000);

        X500Name issuerName = new JcaX509CertificateHolder(caCertificate).getSubject();
        X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(issuerName, now);
        crlBuilder.setNextUpdate(nextUpdate);

        if(crl != null) {
            crlBuilder.addCRL(new JcaX509CRLHolder(crl));
        }

        if (revokedSerial != null) {
            crlBuilder.addCRLEntry(revokedSerial, now, org.bouncycastle.asn1.x509.CRLReason.unspecified);
        }

        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .setProvider("BC")
                .build(keyPair.getPrivate());

        X509CRLHolder crlHolder = crlBuilder.build(signer);
        crl = new JcaX509CRLConverter()
                .setProvider("BC")
                .getCRL(crlHolder);

        saveCRL();
    }

    private void initEmptyCRL() throws Exception {
        updateCRL(null);
    }

    private void loadCRL() throws Exception {
        try (FileInputStream is = new FileInputStream(crlPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
            crl = (X509CRL) cf.generateCRL(is);
        }
    }

    private void saveCRL() throws Exception {
        try (FileOutputStream os = new FileOutputStream(crlPath)) {
            os.write(crl.getEncoded());
        }
    }

    private void incrementSerialNumber() throws IOException {
        serialCounter.incrementAndGet();
        saveSerialCounter();
    }

    protected void loadSerialCounter() throws IOException {
        File f = new File(serialCounterPath);
        if(f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                serialCounter.set(Long.parseLong(br.readLine().trim()));
            }
        } else {
            serialCounter.set(1L);
            saveSerialCounter();
        }
    }

    private void saveSerialCounter() throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(serialCounterPath))) {
            bw.write(Long.toString(serialCounter.get()));
        }
    }

    public BigInteger getNextSerialNumber() {
        return BigInteger.valueOf(serialCounter.get());
    }

    public X509Certificate getCaCertificate() {
        return caCertificate;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public String getName() {
        return name;
    }
}
