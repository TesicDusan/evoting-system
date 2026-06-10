package org.unibl.etf.krz.evoting.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PollResult {

    private String pollId;
    private String pollName;
    private Map<String, Integer> results;
    private int totalVotes;
    private LocalDateTime countTime;

    public PollResult(String pollId, String pollName) {
        this.pollId = pollId;
        this.pollName = pollName;
        this.results = new HashMap<>();
        this.totalVotes = 0;
        this.countTime = LocalDateTime.now();
    }

    public PollResult() {}

    public void initializeOptions(List<String> options) {
        for (String option : options) {
            results.put(option, 0);
        }
    }

    public void addVote(String option) {
        if (!results.containsKey(option)) {
            throw new IllegalArgumentException("Non-existant option: " + option);
        }
        results.put(option, results.get(option) + 1);
        totalVotes++;
    }

    public String getWinner() {
        return results.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    public double getVotePercentage(String option) {
        if (totalVotes == 0) return 0.0;
        return Math.round((results.getOrDefault(option, 0) * 100.0 / totalVotes) * 100.0) / 100.0;
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("poll:").append(pollId).append("\n");
        sb.append("name:").append(pollName).append("\n");
        sb.append("countTime").append(countTime).append("\n");
        sb.append("totalVotes").append(totalVotes).append("\n");
        results.forEach((option, votes) ->
                sb.append(option).append(":").append(votes).append("\n")
        );
        return sb.toString();


    }

    public String getPollId() {
        return pollId;
    }

    public String getPollName() {
        return pollName;
    }

    public Map<String, Integer> getResults() {
        return results;
    }

    public int getTotalVotes() {
        return totalVotes;
    }

    public LocalDateTime getCountTime() {
        return countTime;
    }
}
