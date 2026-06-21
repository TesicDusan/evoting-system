package org.unibl.etf.krz.evoting;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.unibl.etf.krz.evoting.ca.CAInitializer;
import org.unibl.etf.krz.evoting.gui.MainFrame;
import org.unibl.etf.krz.evoting.service.*;
import org.unibl.etf.krz.evoting.storage.DataStore;

import javax.swing.*;
import javax.xml.crypto.Data;
import java.security.Security;

public class App 
{
    public static void main(String[] args) throws Exception
    {
        Security.addProvider(new BouncyCastleProvider());
        System.out.println("E-Voting system starting...");

        DataStore.initialize();
        CAInitializer.initialize();
        System.out.println("CA hierarchy ready.");

        RegistrationService registrationService = new RegistrationService(CAInitializer.getOrganizerCA(), CAInitializer.getVoterCA());
        VotingService votingService = new VotingService(CAInitializer.getOrganizerCA());
        PollService pollService = new PollService(votingService);
        AuthService authService = new AuthService(CAInitializer.getOrganizerCA(), CAInitializer.getVoterCA(), pollService);
        ReportService reportService = new ReportService(pollService);

        SwingUtilities.invokeLater(() -> new MainFrame(registrationService, authService, votingService, pollService, reportService));
    }
}
