package org.unibl.etf.krz.evoting.crypto;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class CryptoUtil {

    private static final String PROVIDER = "BC";

    private static final String RSA_ALGORITHM = "RSA";
    private static final int RSA_KEY_SIZE = 2048;

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int AES_KEY_SIZE = 256;
    private static final int AES_IV_SIZE = 16;

    private static final String RSA_WRAP_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256andMGF1Padding";

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_SIZE = 16;

    public static String generateSalt() {
        byte[] salt = new byte[SALT_SIZE];
        new SecureRandom().nextBytes(salt);
        return bytesToHex(salt);
    }

    public static String hashPassword(String password, String saltHex) throws NoSuchAlgorithmException {
        byte[] salt = hexToBytes(saltHex);
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
        md.update(salt);
        byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    public static boolean verifyPassword(String password, String saltHex, String expectedHashHex) throws NoSuchAlgorithmException {
        String actualHash = hashPassword(password, saltHex);
        return actualHash.equals(expectedHashHex);
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(RSA_ALGORITHM, PROVIDER);
        kpg.initialize(RSA_KEY_SIZE, new SecureRandom());
        return kpg.generateKeyPair();
    }

    public static PKCS10CertificationRequest buildCSR(String subjectDN, KeyPair keyPair) throws Exception {
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .setProvider(PROVIDER)
                .build(keyPair.getPrivate());
        return new JcaPKCS10CertificationRequestBuilder(new X500Name(subjectDN), keyPair.getPublic())
                .build(signer);
    }

    public static void saveCertificate(X509Certificate certificate, String path) throws CertificateEncodingException, IOException {
        try (FileOutputStream os = new FileOutputStream(path)) {
            os.write(certificate.getEncoded());
        }
    }

    public static X509Certificate loadCertificate(String path) throws CertificateException, IOException, NoSuchProviderException {
        try (FileInputStream is = new FileInputStream(path)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509", PROVIDER);
            return (X509Certificate) cf.generateCertificate(is);
        }
    }

    public static String certificateToBase64(X509Certificate certificate) throws CertificateEncodingException {
        return Base64.getEncoder().encodeToString(certificate.getEncoded());
    }

    public static X509Certificate certificateFromBase64(String base64) throws CertificateException,IOException, NoSuchProviderException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509", PROVIDER);
            return (X509Certificate) cf.generateCertificate(bis);
        }
    }

    public static void saveToKeystore(PrivateKey privateKey, X509Certificate certificate, String alias, String password, String path) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12", PROVIDER);
        keyStore.load(null, null);
        keyStore.setKeyEntry(
                alias,
                privateKey,
                password.toCharArray(),
                new Certificate[]{ certificate }
        );
        try (FileOutputStream os = new FileOutputStream(path)) {
            keyStore.store(os, password.toCharArray());
        }
    }

    public static PrivateKey loadPrivateKeyFromKeystore(String alias, String password, String path) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12", PROVIDER);
        try (FileInputStream is = new FileInputStream(path)) {
            keyStore.load(is, password.toCharArray());
        }
        PrivateKey key = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
        if (key == null) {
            throw new KeyStoreException("Private key not found for alias: " + alias);
        }
        return key;
    }

    public static SecretKey generateAESKey() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM, PROVIDER);
        keyGenerator.init(AES_KEY_SIZE, new SecureRandom());
        return keyGenerator.generateKey();
    }

    public static EncryptionResult encryptAES(byte[] plaintext, SecretKey key) throws Exception {
        byte[] ivBytes = new byte[AES_IV_SIZE];
        new SecureRandom().nextBytes(ivBytes);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION, PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] ciphertext = cipher.doFinal(plaintext);

        return new EncryptionResult(
                Base64.getEncoder().encodeToString(ciphertext),
                Base64.getEncoder().encodeToString(ivBytes)
        );
    }

    public static byte[] decryptAES(String ciphertextBase64, String ivBase64, SecretKey key) throws Exception {
        byte[] ciphertext = Base64.getDecoder().decode(ciphertextBase64);
        byte[] ivBytes = Base64.getDecoder().decode(ivBase64);

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION, PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivBytes));
        return cipher.doFinal(ciphertext);
    }

    public static String aesKeyToBase64(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static SecretKey aesKeyFromBase64(String base64) {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }

    public static String encryptRSA(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_WRAP_TRANSFORMATION, PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(data));
    }

    public static byte[] decryptRSA(String ciphertextBase64, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_WRAP_TRANSFORMATION, PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(Base64.getDecoder().decode(ciphertextBase64));
    }

    public static String sign(byte[] data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, PROVIDER);
        signature.initSign(privateKey);
        signature.update(data);
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    public static boolean verifyBySignature(byte[] data, String signatureBase64, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, PROVIDER);
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(Base64.getDecoder().decode(signatureBase64));
    }

    public static SecretKey generateHMACKey() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(HMAC_ALGORITHM, PROVIDER);
        keyGenerator.init(256, new SecureRandom());
        return keyGenerator.generateKey();
    }

    public static String computeHMAC(byte[] data, SecretKey key) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM, PROVIDER);
        mac.init(key);
        return bytesToHex(mac.doFinal(data));
    }

    public static boolean verifyHMAC(byte[] data, SecretKey key, String expectedHex) throws Exception {
        String actual = computeHMAC(data, key);
        return MessageDigest.isEqual(hexToBytes(actual), hexToBytes(expectedHex));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len/2];
        for (int i = 0; i < len; i += 2) {
            data[i/2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static class EncryptionResult {
        private final String ciphertextBase64;
        private final String ivBase64;

        public EncryptionResult(String ciphertextBase64, String ivBase64) {
            this.ciphertextBase64 = ciphertextBase64;
            this.ivBase64 = ivBase64;
        }

        public String getCiphertextBase64() {
            return ciphertextBase64;
        }

        public String getIvBase64() {
            return ivBase64;
        }
    }
}
