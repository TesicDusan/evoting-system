package org.unibl.etf.krz.evoting.service;

import org.unibl.etf.krz.evoting.ca.OrganizerCA;
import org.unibl.etf.krz.evoting.crypto.CryptoUtil;
import org.unibl.etf.krz.evoting.model.EncryptedVote;
import org.unibl.etf.krz.evoting.model.Poll;
import org.unibl.etf.krz.evoting.model.Vote;
import org.unibl.etf.krz.evoting.model.Voter;
import org.unibl.etf.krz.evoting.storage.DataStore;

import javax.crypto.SecretKey;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class VotingService {

    private static final String DIR_HMAC_KEYS = DataStore.ROOT + "/hmac-keys";

    private final OrganizerCA organizerCA;

    public VotingService(OrganizerCA organizerCA) {
        this.organizerCA = organizerCA;
        new File(DIR_HMAC_KEYS).mkdirs();
    }

    public void protectData(Poll poll) throws Exception {
        SecretKey hmacKey = CryptoUtil.generateHMACKey();
        String hmacKeyBase64 = Base64.getEncoder().encodeToString(hmacKey.getEncoded());
        byte[] data = serializeData(poll);
        String hmac = CryptoUtil.computeHMAC(data, hmacKey);
        Files.writeString(Paths.get(getHmacKeyPath(poll.getPollId())), hmacKeyBase64);
        DataStore.saveHmac(poll.getPollId(), hmac);
    }

    public void checkDataIntegrity(Poll poll) throws Exception {
        String hmacKeyBase64 = Files.readString(Paths.get(getHmacKeyPath(poll.getPollId())));
        SecretKey hmacKey = CryptoUtil.keyFromBase64(hmacKeyBase64, CryptoUtil.HMAC_ALGORITHM);
        byte[] data = serializeData(poll);
        String expectedHmac = DataStore.loadHmac(poll.getPollId());
        boolean valid = CryptoUtil.verifyHMAC(data, hmacKey, expectedHmac);
        if (!valid) {
            throw new SecurityException("Poll data integrity violated: " + poll.getPollId());
        }
    }

    private String getHmacKeyPath(String pollId) {
        return DIR_HMAC_KEYS + "/" + pollId + ".key";
    }

    private byte[] serializeData(Poll poll) {
        String s = "id:" + poll.getPollId() + "\n" +
                    "name:" + poll.getName() + "\n" +
                    "description:" + poll.getDescription() + "\n" +
                    "start:" + poll.getStartTime() + "\n" +
                    "end:" + poll.getEndTime() + "\n" +
                    "options:" + String.join(",", poll.getOptions());
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public String vote(Poll poll, Voter voter, int optionId, X509Certificate orgCert, PrivateKey voterPrivateKey) throws VotingException {

        if (!poll.isActive()) {
            throw new VotingException("Poll is not active");
        }
        if(voter.votedIn(poll.getPollId())) {
            throw new VotingException("You have already voted in this poll.");
        }
        if (optionId < 0 || poll.getOptions().size() < optionId) {
            throw new VotingException("Invalid voting option");
        }

        try {
            Vote vote = new Vote(poll.getPollId(), voter.getUsername(), optionId);
            byte[] voteBytes = vote.serialize().getBytes(StandardCharsets.UTF_8);

            SecretKey aesKey = CryptoUtil.generateAESKey();
            CryptoUtil.EncryptionResult enc = CryptoUtil.encryptAES(voteBytes, aesKey);
            PublicKey orgPublicKey = orgCert.getPublicKey();
            String encKey = CryptoUtil.encryptRSA(aesKey.getEncoded(), orgPublicKey);

            byte[] ciphertextBytes = Base64.getDecoder().decode(enc.getCiphertextBase64());
            String signature = CryptoUtil.sign(ciphertextBytes, voterPrivateKey);

            String receiptHash = sha256Hex(voteBytes);

            EncryptedVote encVote = new EncryptedVote(
                    poll.getPollId(),
                    voter.getUsername(),
                    enc.getCiphertextBase64(),
                    encKey,
                    signature,
                    enc.getIvBase64(),
                    receiptHash
            );
            DataStore.saveEncryptedVote(encVote);

            voter.registerVote(poll.getPollId(), receiptHash);
            DataStore.saveVoter(voter);

            System.out.println("Vote registered for voter: " + voter.getUsername() + "in poll: " + poll.getName());
            return receiptHash;
        } catch (VotingException e) {
            throw e;
        } catch (Exception e) {
            throw new VotingException("Error while voting: " + e.getMessage());
        }
    }

    public boolean verifyVote(Poll poll, Voter voter, String receiptHash, X509Certificate voterCert) throws VotingException {
        try {
            EncryptedVote encVote = DataStore.findVote(voter.getUsername(), poll.getPollId());
            if (encVote == null) {
                throw new VotingException("No vote record found for " + voter.getUsername() + " in " + poll.getName());
            }

            byte[] ciphertextBytes = Base64.getDecoder().decode(encVote.getEncryptedVote());
            boolean signatureValid = CryptoUtil.verifyBySignature(ciphertextBytes, encVote.getDigitalSignature(), voterCert.getPublicKey());
            if (!signatureValid) {
                System.out.println("Invalid signature for voter: " + voter.getUsername());
                return false;
            }

            boolean receiptMatches = encVote.getVoteHash().equals(receiptHash);
            if (!receiptMatches) {
                System.out.println("Receipt hash not matching.");
                return false;
            }

            return true;
        } catch (VotingException e) {
            throw e;
        } catch (Exception e) {
            throw new VotingException("Error during vote verification: " + e.getMessage());
        }
    }

    private String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return CryptoUtil.bytesToHex(md.digest(data));
    }

    public static class VotingException extends Exception {
        public VotingException(String message) {
            super(message);
        }
    }
}
