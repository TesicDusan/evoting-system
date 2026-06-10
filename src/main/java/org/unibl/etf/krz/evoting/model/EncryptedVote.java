package org.unibl.etf.krz.evoting.model;

import java.util.UUID;

public class EncryptedVote {

    private String eVoteId;
    private String pollId;
    private String voterId;
    private String encryptedVote;
    private String encryptedSymKey;
    private String digitalSignature;
    private String iv;
    private String voteHash;

    public EncryptedVote(String pollId, String voterId, String encryptedVote, String encryptedSymKey, String digitalSignature, String iv, String voteHash) {
        this.eVoteId = UUID.randomUUID().toString();
        this.pollId = pollId;
        this.voterId = voterId;
        this.encryptedVote = encryptedVote;
        this.encryptedSymKey = encryptedSymKey;
        this.digitalSignature = digitalSignature;
        this.iv = iv;
        this.voteHash = voteHash;
    }

    public EncryptedVote() {}

    public String geteVoteId() {
        return eVoteId;
    }

    public String getPollId() {
        return pollId;
    }

    public String getVoterId() {
        return voterId;
    }

    public String getEncryptedVote() {
        return encryptedVote;
    }

    public String getEncryptedSymKey() {
        return encryptedSymKey;
    }

    public String getDigitalSignature() {
        return digitalSignature;
    }

    public String getIv() {
        return iv;
    }

    public String getVoteHash() {
        return voteHash;
    }
}
