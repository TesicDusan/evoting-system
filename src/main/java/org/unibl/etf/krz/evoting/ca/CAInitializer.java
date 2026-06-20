package org.unibl.etf.krz.evoting.ca;

import java.io.File;
public class CAInitializer {

    private static RootCA rootCA;
    private static OrganizerCA organizerCA;
    private static VoterCA voterCA;

    public static void initialize() throws Exception {

        new File("data/ca").mkdirs();

        rootCA = new RootCA(
                "data/ca/root.p12",
                "data/ca/root.crl",
                "data/ca/root_serial.txt",
                "rootpass"
        );

        if (keystoreExists("data/ca/root.p12")) {
            rootCA.loadFromKeystore();
        } else {
            rootCA.bootstrap();
            rootCA.saveToKeystore();
        }

        organizerCA = new OrganizerCA(
                "data/ca/organizer.p12",
                "data/ca/organizer.crl",
                "data/ca/organizer_serial.txt",
                "orgpass",
                rootCA.getCaCertificate()
        );

        if (keystoreExists("data/ca/organizer.p12")) {
            organizerCA.loadFromKeystore();
        } else {
            organizerCA.bootstrap(rootCA);
            organizerCA.saveToKeystore();
        }

        voterCA = new VoterCA(
                "data/ca/voter.p12",
                "data/ca/voter.crl",
                "data/ca/voter_serial.txt",
                "voterpass",
                rootCA.getCaCertificate()
        );

        if (keystoreExists("data/ca/voter.p12")) {
            voterCA.loadFromKeystore();
        } else {
            voterCA.bootstrap(rootCA);
            voterCA.saveToKeystore();
        }
    }

    private static boolean keystoreExists(String path) {
        File f = new File(path);
        return f.exists() && f.length() > 0;
    }

    public static RootCA getRootCA() {
        return rootCA;
    }

    public static OrganizerCA getOrganizerCA() {
        return organizerCA;
    }

    public static VoterCA getVoterCA() {
        return voterCA;
    }
}
