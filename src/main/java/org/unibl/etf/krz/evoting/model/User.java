package org.unibl.etf.krz.evoting.model;

import java.time.LocalDateTime;

public abstract class User {

    protected String username;
    protected String passwordHash;
    protected String passwordSalt;
    protected String certPath;
    protected String keystorePath;
    protected int failedAttempts;
    protected boolean revoked;
    protected LocalDateTime registrationDate;

    protected User(String username, String passwordHash, String passwordSalt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.failedAttempts = 0;
        this.revoked = false;
        this.registrationDate = LocalDateTime.now();
    }

    protected User() {}

    public abstract String getUserType();

    public void registerFailedAttempt() {
        this.failedAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }

    public boolean toBeRevoked() {
        return failedAttempts >= 3;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getCertPath() {
        return certPath;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }
}
