package org.unibl.etf.krz.evoting.service;

import org.unibl.etf.krz.evoting.crypto.CryptoUtil;
import org.unibl.etf.krz.evoting.model.*;
import org.unibl.etf.krz.evoting.storage.DataStore;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

public class ReportService {

    private final PollService pollService;

    public ReportService(PollService pollService) {
        this.pollService = pollService;
    }

    public PollReport countVotes(Poll poll, Organizer organizer, PrivateKey orgPrivateKey) throws ReportException {
        poll.refreshStatus();
        if (poll.isCounted()) {
            throw new ReportException("Poll has already been counted: " + poll.getPollId());
        }
        if (!poll.isFinished()) {
            throw new ReportException("Polling is not finished, finishes at: " + poll.getEndTime());
        }
        if (!organizer.getUserId().equals(poll.getOrgId())) {
            throw new ReportException("Only poll organizer can count the poll.");
        }

        try {
            List<EncryptedVote> votes = DataStore.loadPollVotes(poll.getPollId());
            PollResult result = new PollResult(poll.getPollId(), poll.getName());
            result.initializeOptions(poll.getOptions());

            int valid = 0;
            int invalid = 0;

            for (EncryptedVote vote : votes) {
                try {
                    String selectedOption = processVote(vote, orgPrivateKey, poll);
                    result.addVote(selectedOption);
                    valid++;
                } catch (ReportException e) {
                    invalid++;
                    System.out.println("Skipped invalid vote with id " + vote.geteVoteId() + " : " + e.getMessage());
                }
            }

            System.out.println("Valid votes: " + valid + ", invalid votes: " + invalid);

            PollReport report = new PollReport(organizer.getUserId(), organizer.getOrgName(), result);
            String content = report.buildContentToSign();
            String signature = CryptoUtil.sign(content.getBytes(StandardCharsets.UTF_8), orgPrivateKey);
            report.setSignature(content, signature);
            DataStore.saveReport(report);
            pollService.markCounted(poll);

            System.out.println("Report generated and signed for poll: " + poll.getName());
            return report;
        } catch (ReportException e) {
            throw e;
        } catch (Exception e) {
            throw new ReportException("Error while counting votes: " + e.getMessage());
        }
    }

    private String processVote(EncryptedVote vote, PrivateKey orgPrivateKey, Poll poll) throws Exception {

        byte[] aesKeyBytes = CryptoUtil.decryptRSA(vote.getEncryptedSymKey(), orgPrivateKey);
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
        X509Certificate voterCert = CryptoUtil.loadCertificate(DataStore.getCertificatePath(vote.getVoterId()));
        byte[] ciphertextBytes = Base64.getDecoder().decode(vote.getEncryptedVote());

        boolean signatureValid = CryptoUtil.verifyBySignature(ciphertextBytes, vote.getDigitalSignature(), voterCert.getPublicKey());
        if (!signatureValid) {
            throw new SecurityException("Voters digital signature is not valid.");
        }

        byte[] plaintext = CryptoUtil.decryptAES(vote.getEncryptedVote(), vote.getIv(), aesKey);
        String serialized = new String(plaintext, StandardCharsets.UTF_8);
        int optionId = parseOptionId(serialized);
        if (optionId < 0 || poll.getOptions().size() < optionId) {
            throw new IllegalArgumentException("Option index out of bounds: " + optionId);
        }
        return poll.getOptions().get(optionId);
    }

    private int parseOptionId(String serialized) {
        String[] parts = serialized.split("\\|");
        for (String part : parts) {
            if (part.startsWith("option:")) {
                return Integer.parseInt(part.substring("option:".length()));
            }
        }
        throw new IllegalArgumentException("Parsing option index not possible.");
    }

    public boolean verifyReport(PollReport report, X509Certificate orgCert) throws ReportException {
        if (!report.isSigned()) {
            System.out.println("Report content has not been signed.");
            return false;
        }
        String content = report.buildContentToSign();
        if (!content.equals(report.getSignedContent())) {
            System.out.println("Report content has been changed after signing");
            return false;
        }
        try {
            return CryptoUtil.verifyBySignature(report.getSignedContent().getBytes(StandardCharsets.UTF_8), report.getDigitalSignature(), orgCert.getPublicKey());
        } catch (Exception e) {
            throw new ReportException("Error during report verification: " + e.getMessage());
        }
    }

    public class ReportException extends Exception {
        public ReportException(String message) {
            super(message);
        }
    }
}
