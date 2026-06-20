package org.unibl.etf.krz.evoting.service;

import org.unibl.etf.krz.evoting.model.Organizer;
import org.unibl.etf.krz.evoting.model.Poll;
import org.unibl.etf.krz.evoting.storage.DataStore;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class PollService {

    private final VotingService votingService;

    public PollService(VotingService votingService) {
        this.votingService = votingService;
    }

    public Poll createPoll(Organizer organizer, String name, String description, LocalDateTime startTime, LocalDateTime endTime, List<String> options) throws PollException {
        validateNotEmpty(name, "Name");
        validateNotEmpty(description, "Description");
        if (startTime == null || endTime == null) {
            throw new PollException("Start and end times must be defined.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new PollException("End time must be after start time.");
        }
        if (endTime.isBefore(LocalDateTime.now())) {
            throw new PollException("End time must not have passed.");
        }
        if (options == null || options.size() < 2 || options.size() > 5) {
            throw new PollException("Poll must have 2 to 5 options.");
        }
        for (String option : options) {
            validateNotEmpty(option, "Option name");
        }
        if (options.size() != options.stream().distinct().count()) {
            throw new PollException("Options must be unique.");
        }

        try {
            Poll poll = new Poll(
                    organizer.getUserId(),
                    organizer.getUsername(),
                    name,
                    description,
                    startTime,
                    endTime,
                    options
            );
            poll.refreshStatus();
            DataStore.savePoll(poll);
            votingService.protectData(poll);
            organizer.addPoll(poll.getPollId());
            DataStore.saveOrganizer(organizer);

            System.out.println("New poll created: " + name);
            return poll;
        } catch (PollException e) {
            throw e;
        } catch (Exception e) {
            throw new PollException("Error during poll creation: " + e.getMessage());
        }
    }

    public List<Poll> listOrganizerPolls(Organizer organizer) throws PollException {
        try {
            return DataStore.loadOrganizerPolls(organizer.getUserId());
        } catch (Exception e) {
            throw new PollException("Error while loading polls: " + e.getMessage());
        }
    }

    public List<Poll> listActivePolls() throws PollException {
        try {
            return DataStore.loadPollsStatus(Poll.Status.ACTIVE);
        } catch (Exception e) {
            throw new PollException("Error while loading active polls: " + e.getMessage());
        }
    }

    public Poll loadPoll(String pollId) throws PollException {
        try {
            Poll poll = DataStore.loadPoll(pollId);
            if (poll == null) {
                throw new PollException("Poll not found: " + pollId);
            }

            votingService.checkDataIntegrity(poll);

            poll.refreshStatus();
            return poll;
        } catch (PollException e) {
            throw e;
        } catch (Exception e) {
            throw new PollException("Error while loading poll: " + e.getMessage());
        }
    }

    public void markCounted(Poll poll) throws PollException {
        try {
            poll.markCounted();
            DataStore.savePoll(poll);
        } catch (Exception e) {
            throw new PollException("Error while updating poll status: " + e.getMessage());
        }
    }

    private void validateNotEmpty(String value, String name) throws PollException {
        if (value == null || value.isBlank()) {
            throw new PollException(name + " must not be empty.");
        }
    }

    public class PollException extends Exception {
        public PollException(String message) {
            super(message);
        }
    }
}
