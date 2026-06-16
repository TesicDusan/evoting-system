package org.unibl.etf.krz.evoting.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Voter extends User {

    private String firstName;
    private String lastName;
    private Map<String, String> votedInPolls;

    public Voter(String username, String passwordHash, String passwordSalt, String firstName, String lastName) {
        super(username, passwordHash, passwordSalt);
        this.firstName = firstName;
        this.lastName = lastName;
        this.votedInPolls = new HashMap<>();
    }

    public Voter() {
        super();
        this.votedInPolls = new HashMap<>();
    }

    @Override
    public String getUserType() {
        return "VOTER";
    }

    public void registerVote(String pollId, String receiptHash) {
        this.votedInPolls.put(pollId, receiptHash);
    }

    public boolean votedIn(String pollId) {
        return this.votedInPolls.containsKey(pollId);
    }

    public String getReceiptForPoll(String pollId) {
        return votedInPolls.get(pollId);
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + "" + lastName;
    }

    public Map<String, String> getVotedInPolls() {
        return votedInPolls;
    }
}
