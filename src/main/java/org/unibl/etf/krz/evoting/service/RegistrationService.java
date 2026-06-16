package org.unibl.etf.krz.evoting.service;

import org.bouncycastle.asn1.cmc.CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.unibl.etf.krz.evoting.ca.OrganizerCA;
import org.unibl.etf.krz.evoting.ca.VoterCA;
import org.unibl.etf.krz.evoting.crypto.CryptoUtil;
import org.unibl.etf.krz.evoting.model.Organizer;
import org.unibl.etf.krz.evoting.model.Voter;
import org.unibl.etf.krz.evoting.storage.DataStore;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegistrationService {

    private final OrganizerCA organizerCA;
    private final VoterCA voterCA;

    public RegistrationService(OrganizerCA organizerCA, VoterCA voterCA) {
        this.organizerCA = organizerCA;
        this.voterCA = voterCA;
    }

    public Organizer registerOrg(String username, String password, String orgName, String orgId) throws RegistrationException {

        validateUsername(username);
        validatePassword(password);
        validateNotEmpty(orgName, "Organization name");

        try {
            String salt = CryptoUtil.generateSalt();
            String hash = CryptoUtil.hashPassword(password, salt);

            Organizer organizer = new Organizer(username, hash, salt, orgName, orgId);
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String subjectDN = "CN=" + orgName + ", O=" + orgId + ", OU=Organizer" + ", SERIALNUMBER=" + username;
            PKCS10CertificationRequest csr = CryptoUtil.buildCSR(subjectDN, keyPair);

            X509Certificate cert = organizerCA.issueOrganizerCertificate(csr);

            String certPath = DataStore.getCertificatePath(username);
            String keystorePath = DataStore.getKeystorePath(username);

            CryptoUtil.saveCertificate(cert, certPath);
            CryptoUtil.saveToKeystore(keyPair.getPrivate(), cert, username, password, keystorePath);

            organizer.setCertPath(certPath);
            organizer.setKeystorePath(keystorePath);
            DataStore.saveOrganizer(organizer);

            System.out.println("Organizer registered successfully: " + username);
            return organizer;
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistrationException("Registration error: " + e.getMessage(), e);
        }
    }

    public Voter registerVoter(String username, String password, String firstName, String lastName) throws RegistrationException {

        validateUsername(username);
        validatePassword(password);
        validateNotEmpty(firstName, "First name");
        validateNotEmpty(lastName, "Last name");

        try {
            String salt = CryptoUtil.generateSalt();
            String hash = CryptoUtil.hashPassword(password, salt);

            Voter voter = new Voter(username, hash, salt, firstName, lastName);
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String subjectDN = "CN=" + firstName + " " + lastName + ", OU=Voter" + ", SERIALNUMBER=" + username;
            PKCS10CertificationRequest csr = CryptoUtil.buildCSR(subjectDN, keyPair);

            X509Certificate cert = voterCA.issueVoterCertificate(csr);

            String certPath = DataStore.getCertificatePath(username);
            String keystorePath = DataStore.getKeystorePath(username);

            CryptoUtil.saveCertificate(cert, certPath);
            CryptoUtil.saveToKeystore(keyPair.getPrivate(), cert, username, password, keystorePath);

            voter.setCertPath(certPath);
            voter.setKeystorePath(keystorePath);
            DataStore.saveVoter(voter);

            System.out.println("Voter registered successfully: " + username);
            return voter;
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistrationException("Registration error: " + e.getMessage(), e);
        }
    }

    private void validateUsername(String username) throws RegistrationException {
        validateNotEmpty(username, "Username");

        if (username.length() < 3) {
            throw new RegistrationException("Username must contain at least 3 characters.");
        }

        if (!username.matches("[a-zA-Z0-9_]+")) {
            throw new RegistrationException("Username can only contain letters, numbers and _");
        }

        if (DataStore.userExists(username)) {
            throw new RegistrationException("Username is already taken.");
        }
    }

    private void validatePassword(String password) throws RegistrationException {
        validateNotEmpty(password, "Password");

        if (password.length() < 6) {
            throw new RegistrationException("Password must contain at least 6 characters.");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new RegistrationException("Password must contain at least one capital letter.");
        }

        if(!password.matches(".*[!@#$%^&*].*")) {
            throw new RegistrationException("Password must contain at least one special character: !,@,#,$,%,^,&,*");
        }
    }

    private void validateNotEmpty(String value, String name) throws RegistrationException {
        if (value == null || value.isBlank()) {
            throw new RegistrationException(name + "must not be empty.");
        }
    }

    public static class RegistrationException extends Exception {

        public RegistrationException(String message) {
            super(message);
        }

        public RegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
