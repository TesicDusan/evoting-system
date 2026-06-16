package org.unibl.etf.krz.evoting.service;

import java.security.cert.X509Certificate;

public class LoginSession {

    private final X509Certificate userCertificate;
    private final String username;
    private final String userType;

    public LoginSession(X509Certificate userCertificate, String username, String userType) {
        this.userCertificate = userCertificate;
        this.username = username;
        this.userType = userType;
    }

    public X509Certificate getUserCertificate() {
        return userCertificate;
    }

    public String getUsername() {
        return username;
    }

    public String getUserType() {
        return userType;
    }
}
