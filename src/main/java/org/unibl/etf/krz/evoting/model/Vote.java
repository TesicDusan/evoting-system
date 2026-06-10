package org.unibl.etf.krz.evoting.model;

public class Vote {

    private final String voteId;
    private final String voterId;
    private final int optionIndex;

    public Vote(String voteId, String voterId, int optionIndex) {
        this.voteId = voteId;
        this.voterId = voterId;
        this.optionIndex = optionIndex;
    }

    public String getVoteId() {
        return voteId;
    }

    public String getVoterId() {
        return voterId;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    public String serialize() {
        return "vote:" + voteId +
                "|voter:" + voterId +
                "|option:" + optionIndex;
    }
}
