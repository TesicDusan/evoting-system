package org.unibl.etf.krz.evoting.ca;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

public class RootCA extends CA {

    public RootCA(String keystorePath, String clrPath, String serialCounterPath, String keystorePassword) {
        super("Root CA", keystorePath, clrPath, serialCounterPath, keystorePassword);
    }

    public void bootstrap() throws Exception {

        loadSerialCounter();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM, "BC");
        kpg.initialize(KEY_SIZE, new SecureRandom());
        keyPair = kpg.generateKeyPair();

        X500Name subject = new X500Name("CN=Root CA, O=E-Voting System, OU=Root CA");

        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + CA_CERT_VALIDITY_MS);

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                subject,
                BigInteger.ONE,
                notBefore,
                notAfter,
                subject,
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        );

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

        certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(1)
        );

        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)
        );

        certBuilder.addExtension(
                Extension.subjectKeyIdentifier,
                false,
                extUtils.createSubjectKeyIdentifier(keyPair.getPublic())
        );

        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .setProvider("BC")
                .build(keyPair.getPrivate());

        caCertificate = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certBuilder.build(signer));
    }

    @Override
    protected void validateRequest(PKCS10CertificationRequest csr) throws Exception {
        String subject = csr.getSubject().toString();
        if (!subject.contains("OU=CA")) {
            throw new IllegalArgumentException("Root CA only issues certificates to subordinate CAs.");
        }
    }

    @Override
    protected ExtensionsGenerator getExtensions() throws Exception {
        ExtensionsGenerator extGen = new ExtensionsGenerator();

        extGen.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(0)
        );

        extGen.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)
        );

        return extGen;
    }

    public X509Certificate issueSubordinateCACertificate(PKCS10CertificationRequest csr) throws Exception {
        return grantCertificate(csr, CA_CERT_VALIDITY_MS);
    }
}
