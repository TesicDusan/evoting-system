package org.unibl.etf.krz.evoting.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Poll {

    public enum Status {
        UPCOMING,
        ACTIVE,
        FINISHED,
        COUNTED
    }

    private String pollId;
    private String orgId;
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> options;
    private Status status;
    private boolean counted;

    public Poll(String orgId, String name, String description, LocalDateTime startTime, LocalDateTime endTime, List<String> options) {
        this.pollId = UUID.randomUUID().toString();
        this.orgId = orgId;
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.options = new ArrayList<>(options);
        this.status = Status.UPCOMING;
        this.counted = false;
    }

    public Poll() {
        this.options = new ArrayList<>();
    }

    public void refreshStatus() {
        if (counted) {
            this.status = Status.COUNTED;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(endTime)) {
            if (now.isBefore(startTime)) {
                this.status = Status.UPCOMING;
            } else {
                this.status = Status.ACTIVE;
            }
        } else {
            this.status = Status.FINISHED;
        }
    }

    public boolean isActive() {
        refreshStatus();
        return this.status == Status.ACTIVE;
    }

    public boolean isFinished() {
        refreshStatus();
        return this.status == Status.FINISHED;
    }

    public boolean isCounted() {
        return counted;
    }

    public void markCounted() {
        this.counted = true;
        this.status = Status.COUNTED;
    }

    public boolean hasValidNumOfOptions() {
        return this.options.size() >= 2 && this.options.size() <= 5;
    }

    public String getPollId() {
        return pollId;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<String> getOptions() {
        return options;
    }

    public Status getStatus() {
        return status;
    }
}
