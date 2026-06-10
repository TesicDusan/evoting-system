package org.unibl.etf.krz.evoting.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class PollReport {

    private String reportId;
    private String orgId;
    private String orgName;
    private PollResult result;
    private LocalDateTime creationTime;
    private String signedContent;
    private String digitalSignature;
    private String reportPath;

    public PollReport(String orgId, String orgName, PollResult result) {
        this.reportId = UUID.randomUUID().toString();
        this.orgId = orgId;
        this.orgName = orgName;
        this.result = result;
        this.creationTime = LocalDateTime.now();
    }

    public PollReport() {}

    public String buildContentToSign() {
        return "reportId:" + reportId + "\n" +
                "organizerId:" + orgId + "\n" +
                "organizerName:" + orgName + "\n" +
                "creationTime:" + creationTime + "\n" +
                result.serialize();
    }

    public void setSignature(String signedContent, String digitalSignature) {
        this.signedContent = signedContent;
        this.digitalSignature = digitalSignature;
    }

    public boolean isSigned() {
        return digitalSignature != null && !digitalSignature.isEmpty();
    }

    public String displayForm() {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("                 POLL REPORT\n");
        sb.append("====================================================\n");
        sb.append("Report ID: ").append(reportId).append("\n");
        sb.append("Organizer: ").append(orgName).append("\n");
        sb.append("Poll: ").append(result.getPollName()).append("\n");
        sb.append("Time of counting: ").append(result.getCountTime()).append("\n");
        sb.append("Time of report creation: ").append(creationTime).append("\n");
        sb.append("----------------------------------------------------\n");
        sb.append("RESULTS:\n");
        result.getResults().forEach((option, votes) -> {
            sb.append(String.format(" %-30s %d votes (%.2f%%)\n",
                    option,
                    votes,
                    result.getVotePercentage(option)));
        });
        sb.append("----------------------------------------------------\n");
        sb.append("Total number of votes: ").append(result.getTotalVotes()).append("\n");
        sb.append("Winner: ").append(result.getWinner()).append("\n");
        sb.append("====================================================\n");
        sb.append("Digital signature: ").append(
                isSigned() ? "[VALID]" : "[UNSIGNED]"
        ).append("\n");
        return sb.toString();
    }

    public String getReportId() {
        return reportId;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public PollResult getResult() {
        return result;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public String getSignedContent() {
        return signedContent;
    }

    public String getDigitalSignature() {
        return digitalSignature;
    }

    public String getReportPath() {
        return reportPath;
    }

    public void setReportPath(String path) {
        this.reportPath = reportPath;
    }
}
