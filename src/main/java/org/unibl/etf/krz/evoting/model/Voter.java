package org.unibl.etf.krz.evoting.model;

import java.util.ArrayList;
import java.util.List;

public class Voter extends User {

    private String firstName;
    private String lastName;
    private List<String> castVotesIds;

    public Voter(String username, String passwordHash, String passwordSalt, String firstName, String lastName) {
        super(username, passwordHash, passwordSalt);
        this.firstName = firstName;
        this.lastName = lastName;
        this.castVotesIds = new ArrayList<>();
    }

    public Voter() {
        super();
        this.castVotesIds = new ArrayList<>();
    }

    @Override
    public String getUserType() {
        return "VOTER";
    }

    public void redisterVote(String pollId) {
        this.castVotesIds.add(pollId);
    }

    public boolean votedIn(String pollId) {
        return this.castVotesIds.contains(pollId);
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

    public List<String> getCastVotesIds() {
        return castVotesIds;
    }
}
