package org.unibl.etf.krz.evoting.model;

import java.util.ArrayList;
import java.util.List;

public class Organizer extends User {

    private String orgName;
    private List<String> pollIDs;

    public Organizer(String username, String passwordHash, String passwordSalt, String orgName) {
        super(username, passwordHash, passwordSalt);
        this.orgName = orgName;
        this.pollIDs = new ArrayList<>();
    }

    public Organizer() {
        super();
        this.pollIDs = new ArrayList<>();
    }

    @Override
    public String getUserType() {
        return "ORGANIZER";
    }

    public void addPoll(String pollId) {
        this.pollIDs.add(pollId);
    }

    public void removePoll(String pollId) {
        this.pollIDs.remove(pollId);
    }

    public String getOrgName() {
        return orgName;
    }

    public List<String> getPollIDs() {
        return pollIDs;
    }
}
