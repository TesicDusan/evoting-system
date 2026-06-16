package org.unibl.etf.krz.evoting.model;

public class Vote {

    private final String pollId;
    private final String voterId;
    private final int optionIndex;

    public Vote(String pollId, String voterId, int optionIndex) {
        this.pollId = pollId;
        this.voterId = voterId;
        this.optionIndex = optionIndex;
    }

    public String getPollId() {
        return pollId;
    }

    public String getVoterId() {
        return voterId;
    }

    public int getOptionIndex() {
        return optionIndex;
    }

    public String serialize() {
        return "poll:" + pollId +
                "|voter:" + voterId +
                "|option:" + optionIndex;
    }
}
