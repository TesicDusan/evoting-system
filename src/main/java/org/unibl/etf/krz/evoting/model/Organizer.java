package org.unibl.etf.krz.evoting.model;

import java.util.ArrayList;
import java.util.List;

public class Organizer extends User {

    private String orgName;
    private String orgId;
    private List<String> pollIDs;

    public Organizer(String username, String passwordHash, String passwordSalt, String orgName, String orgId) {
        super(username, passwordHash, passwordSalt);
        this.orgName = orgName;
        this.orgId = orgId;
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

    public String getOrgId() {
        return orgId;
    }

    public List<String> getPollIDs() {
        return pollIDs;
    }
}
