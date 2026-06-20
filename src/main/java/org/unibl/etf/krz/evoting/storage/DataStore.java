package org.unibl.etf.krz.evoting.storage;

import com.google.gson.*;
import org.unibl.etf.krz.evoting.model.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataStore {

    private static final String SEPARATOR = "/";
    private static final String EXT_JSON = ".json";
    private static final String EXT_HMAC = ".hmac";
    private static final String EXT_CERTIFICATE = ".cer";
    private static final String EXT_KEYSTORE = ".p12";
    
    public static final String ROOT = "data";
    public static final String DIR_CA = ROOT + "/ca";
    public static final String DIR_ORGANIZERS = ROOT + "/users/organizers";
    public static final String DIR_VOTERS = ROOT + "/users/voters";
    public static final String DIR_CERTIFICATES = ROOT + "/certificates";
    public static final String DIR_KEYSTORES = ROOT + "/keystores";
    public static final String DIR_POLLS = ROOT + "/polls";
    public static final String DIR_VOTES = ROOT + "/votes";
    public static final String DIR_HMAC = ROOT + "/hmac";
    public static final String DIR_REPORTS = ROOT + "/reports";

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public static void initialize() {
        String[] dirs = {
                DIR_CA, DIR_ORGANIZERS, DIR_VOTERS, DIR_CERTIFICATES,
                DIR_KEYSTORES, DIR_POLLS, DIR_VOTES, DIR_HMAC, DIR_REPORTS
        };
        for (String dir : dirs) {
            new File(dir).mkdirs();
        }
    }

    public static void saveOrganizer(Organizer organizer) throws IOException {
        String path = DIR_ORGANIZERS + SEPARATOR + organizer.getUsername() + EXT_JSON;
        writeJson(path, organizer);
    }

    public static Organizer loadOrganizer(String username) throws IOException {
        String path = DIR_ORGANIZERS + SEPARATOR + username + EXT_JSON;
        return readJson(path, Organizer.class);
    }

    public static List<Organizer> loadAllOrganizers() throws IOException {
        return loadAll(DIR_ORGANIZERS, Organizer.class);
    }

    public static boolean organizerExists(String username) {
        return new File(DIR_ORGANIZERS + SEPARATOR + username + EXT_JSON).exists();
    }

    public static void saveVoter(Voter voter) throws IOException {
        String path = DIR_VOTERS + SEPARATOR + voter.getUsername() + EXT_JSON;
        writeJson(path, voter);
    }

    public static Voter loadVoter(String username) throws IOException {
        String path = DIR_VOTERS + SEPARATOR + username + EXT_JSON;
        return readJson(path, Voter.class);
    }

    public static List<Voter> loadAllVoters() throws IOException {
        return loadAll(DIR_VOTERS, Voter.class);
    }

    public static boolean voterExists(String username) {
        return new File(DIR_VOTERS + SEPARATOR + username + EXT_JSON).exists();
    }

    public static User loadUser(String username) throws IOException {
        if (organizerExists(username)) {
            return loadOrganizer(username);
        }
        if (voterExists(username)) {
            return loadVoter(username);
        }
        return null;
    }

    public static boolean userExists(String username) {
        return organizerExists(username) || voterExists(username);
    }

    public static void savePoll(Poll poll) throws IOException {
        String path = DIR_POLLS + SEPARATOR + poll.getPollId() + EXT_JSON;
        writeJson(path, poll);
    }

    public static Poll loadPoll(String pollId) throws IOException {
        String path = DIR_POLLS + SEPARATOR + pollId + EXT_JSON;
        return readJson(path, Poll.class);
    }

    public static List<Poll> loadAllPolls() throws IOException {
        return loadAll(DIR_POLLS, Poll.class);
    }

    public static List<Poll> loadOrganizerPolls(String organizerId) throws IOException {
        return loadAllPolls().stream()
                .filter(poll -> organizerId.equals(poll.getOrgId()))
                .collect(Collectors.toList());
    }

    public static List<Poll> loadPollsStatus(Poll.Status status) throws IOException {
        List<Poll> all = loadAllPolls();
        all.forEach(Poll::refreshStatus);
        return all.stream()
                .filter(poll -> status.equals(poll.getStatus()))
                .collect(Collectors.toList());
    }

    public static void saveEncryptedVote(EncryptedVote vote) throws IOException {
        String dir = DIR_VOTES + SEPARATOR + vote.getPollId();
        new File(dir).mkdirs();
        writeJson(dir + SEPARATOR + vote.geteVoteId() + EXT_JSON, vote);
    }

    public static List<EncryptedVote> loadPollVotes(String pollId) throws IOException {
        String dir = DIR_VOTES + SEPARATOR + pollId;
        return loadAll(dir, EncryptedVote.class);
    }

    public static EncryptedVote findVote(String voterId, String pollId) throws IOException {
        List<EncryptedVote> votes = loadPollVotes(pollId);
        for (EncryptedVote vote : votes) {
            if (voterId.equals(vote.getVoterId())) {
                return vote;
            }
        }
        return null;
    }

    public static void saveHmac(String pollId, String hmacHex) throws IOException {
        String path = DIR_HMAC + SEPARATOR + pollId + EXT_HMAC;
        Files.writeString(Paths.get(path), hmacHex);
    }

    public static String loadHmac(String pollId) throws IOException {
        String path = DIR_HMAC + SEPARATOR  + pollId + EXT_HMAC;
        return Files.readString(Paths.get(path)).trim();
    }

    public static boolean hmacExists(String pollId) {
        return new File(DIR_HMAC + SEPARATOR + pollId + EXT_HMAC).exists();
    }

    public static void saveReport(PollReport report) throws IOException {
        String path = DIR_REPORTS + SEPARATOR + report.getResult().getPollId() + EXT_JSON;
        writeJson(path, report);
    }

    public static PollReport loadReport(String pollId) throws IOException {
        String path = DIR_REPORTS + SEPARATOR + pollId + EXT_JSON;
        return readJson(path, PollReport.class);
    }

    public static boolean reportExists(String pollId) {
        return new File(DIR_REPORTS + SEPARATOR + pollId + EXT_JSON).exists();
    }

    public static String getCertificatePath(String username) {
        return DIR_CERTIFICATES + SEPARATOR + username + EXT_CERTIFICATE;
    }

    public static String getKeystorePath(String username) {
        return DIR_KEYSTORES + SEPARATOR + username + EXT_KEYSTORE;
    }

    private static <T> void writeJson(String path, T object) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            GSON.toJson(object, writer);
        }
    }

    private static <T> T readJson(String path, Class<T> clazz) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return GSON.fromJson(reader, clazz);
        }
    }

    private static <T> List<T> loadAll(String dirPath, Class<T> clazz) throws IOException {
        List<T> results = new ArrayList<>();
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return results;
        }
        File[] files = dir.listFiles(f -> f.getName().endsWith(EXT_JSON));
        if (files == null) return results;
        for (File file : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                T obj = GSON.fromJson(reader, clazz);
                if (obj != null) results.add(obj);
            }
        }
        return results;
    }

    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public JsonElement serialize(LocalDateTime src, Type type, JsonSerializationContext ctx) {
            return new JsonPrimitive(src.format(FORMATTER));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), FORMATTER);
        }
    }
}
