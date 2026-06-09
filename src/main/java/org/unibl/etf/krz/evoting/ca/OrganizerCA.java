package org.unibl.etf.krz.evoting.ca;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class OrganizerCA extends CA {

    private final X509Certificate rootCACertificate;

    public OrganizerCA(String keystorePath, String crlPath, String serialCounterPath, String keystorePassword, X509Certificate rootCACertificate) {
        super("OrganizerCA", keystorePath, crlPath, serialCounterPath, keystorePassword);
        this.rootCACertificate = rootCACertificate;
    }

    public void bootstrap(RootCA rootCA) throws Exception {

        loadSerialCounter();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM, "BC");
        kpg.initialize(KEY_SIZE, new SecureRandom());
        keyPair = kpg.generateKeyPair();

        X500Name subject = new X500Name("CN=Organizer CA, O=E-Voting System, OU=CA");

        PKCS10CertificationRequest csr = buildCSR(subject, keyPair);

        caCertificate = rootCA.issueSubordinateCACertificate(csr);
    }

    private PKCS10CertificationRequest buildCSR(X500Name subject, KeyPair keyPair) throws Exception {
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .setProvider("BC")
                .build(keyPair.getPrivate());

        return new JcaPKCS10CertificationRequestBuilder(
                subject,
                keyPair.getPublic()
        ).build(signer);
    }

    @Override
    protected ExtensionsGenerator getExtensions() throws Exception {
        ExtensionsGenerator extensionsGenerator = new ExtensionsGenerator();

        extensionsGenerator.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(false)
        );

        extensionsGenerator.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
        );

        extensionsGenerator.addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_emailProtection})
        );

        return extensionsGenerator;
    }

    @Override
    protected void validateRequest(PKCS10CertificationRequest csr) throws Exception {
        String subject = csr.getSubject().toString();
        if (!subject.contains("OU=Organizer")) {
            throw new IllegalArgumentException("Organizer CA can only issue certifications to organizer accounts.");
        }
    }

    public X509Certificate issueOrganizerCertificate(PKCS10CertificationRequest csr) throws Exception {
        return grantCertificate(csr, END_ENTITY_VALIDITY_MS);
    }
}
