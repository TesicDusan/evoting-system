package org.unibl.etf.krz.evoting.service;

import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.unibl.etf.krz.evoting.ca.OrganizerCA;
import org.unibl.etf.krz.evoting.ca.VoterCA;
import org.unibl.etf.krz.evoting.crypto.CryptoUtil;
import org.unibl.etf.krz.evoting.model.Organizer;
import org.unibl.etf.krz.evoting.model.User;
import org.unibl.etf.krz.evoting.model.Voter;
import org.unibl.etf.krz.evoting.storage.DataStore;

import java.security.cert.X509Certificate;

public class AuthService {

    private final OrganizerCA organizerCA;
    private final VoterCA voterCA;

    public AuthService(OrganizerCA organizerCA, VoterCA voterCA) {
        this.organizerCA = organizerCA;
        this.voterCA = voterCA;
    }

    public LoginSession validateCertificate(String certPath) throws LoginException {

        X509Certificate certificate;
        try {
            certificate = CryptoUtil.loadCertificate(certPath);
        } catch (Exception e) {
            throw new LoginException("Not possible to load certificate from path: " + certPath);
        }

        try {
            organizerCA.validateCertificate(certificate);
            String username = extractUsername(certificate);
            return new LoginSession(certificate, username, "ORGANIZER");
        } catch (Exception e) {
            //
        }

        try {
            voterCA.validateCertificate(certificate);
            String username = extractUsername(certificate);
            return new LoginSession(certificate, username, "VOTER");
        } catch (Exception e) {
            throw new LoginException("Certificate not valid or revoked");
        }
    }

    public User login(LoginSession session, String username, String password) throws LoginException {

        if (!username.equals(session.getUsername())) {
            throw new LoginException("Username not matching certificate.");
        }

        User user;
        try {
            user = DataStore.loadUser(username);
        } catch (Exception e) {
            throw new LoginException("Error while loading user: " + e.getMessage());
        }
        if (user == null) {
            throw new LoginException("User not found: " + username);
        }
        if (user.isRevoked()) {
            throw new LoginException("User revoked.");
        }

        boolean passwordOk;
        try {
            passwordOk = CryptoUtil.verifyPassword(password, user.getPasswordSalt(), user.getPasswordHash());
        } catch (Exception e) {
            throw new LoginException("Error during password verification: " + e.getMessage());
        }
        if (!passwordOk) {
            user.registerFailedAttempt();
            if (user.toBeRevoked()) {
                revokeAndLock(user, session);
                persistUser(user);
                throw new LoginException("Too many failed attempts. User revoked.");
            }

            persistUser(user);
            int attemptsLeft = 3 - user.getFailedAttempts();
            throw new LoginException("Wrong password. Attempts left: " + attemptsLeft);
        }

        user.resetFailedAttempts();
        persistUser(user);

        System.out.println("Login successful: " + username);
        return user;
    }

    private void revokeAndLock(User user, LoginSession session) throws LoginException {
        try {
            if ("ORGANIZER".equals(session.getUserType())) {
                organizerCA.revokeCertificate(session.getUserCertificate());
            } else {
                voterCA.revokeCertificate(session.getUserCertificate());
            }
            user.setRevoked(true);
            System.out.println("Certificate revoked for user: " + user.getUsername());
        } catch (Exception e) {
            throw new LoginException("Error during certificate revocation: " + e.getMessage());
        }
    }

    private void persistUser(User user) throws LoginException {
        try {
            if ("ORGANIZER".equals(user.getUserType())) {
                DataStore.saveOrganizer((Organizer) user);
            } else {
                DataStore.saveVoter((Voter) user);
            }
        } catch (Exception e) {
            throw new LoginException("Error during user persistence: " + e.getMessage());
        }
    }

    private String extractUsername(X509Certificate certificate) throws LoginException {
        try {
            X500Name subject = new JcaX509CertificateHolder(certificate).getSubject();

            RDN[] serialRDNs = subject.getRDNs(BCStyle.SERIALNUMBER);
            if (serialRDNs != null && serialRDNs.length > 0) {
                AttributeTypeAndValue first = serialRDNs[0].getFirst();
                return first.getValue().toString();
            }

            throw new LoginException("Could not extract username form certificate.");
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new LoginException("Error during reading subject DN from certificate: " + e.getMessage());
        }
    }

    public static class LoginException extends Exception {
        public LoginException(String message) {
            super(message);
        }
    }
}
