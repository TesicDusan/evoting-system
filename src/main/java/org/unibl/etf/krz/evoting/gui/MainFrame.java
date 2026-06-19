package org.unibl.etf.krz.evoting.gui;

import org.unibl.etf.krz.evoting.model.Organizer;
import org.unibl.etf.krz.evoting.model.User;
import org.unibl.etf.krz.evoting.model.Voter;
import org.unibl.etf.krz.evoting.service.*;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public static final String WELCOME_SCREEN = "welcome";
    public static final String REGISTER_SCREEN = "register";
    public static final String CERT_LOGIN_SCREEN = "loginCert";
    public static final String PASS_LOGIN_SCREEN = "loginPass";
    public static final String ORGANIZER_SCREEN = "organizer";
    public static final String VOTER_SCREEN = "voter";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardContainer = new JPanel(cardLayout);

    private final RegistrationService registrationService;
    private final AuthService authService;
    private final VotingService votingService;
    private final PollService pollService;
    private final ReportService reportService;

    public MainFrame(RegistrationService registrationService, AuthService authService, VotingService votingService, PollService pollService, ReportService reportService) {
        super("E-Voting System");
        this.registrationService = registrationService;
        this.authService = authService;
        this.votingService = votingService;
        this.pollService = pollService;
        this.reportService = reportService;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(880, 600));
        setLocationRelativeTo(null);

        add(cardContainer, BorderLayout.CENTER);
        cardContainer.add(new WelcomePanel(this), WELCOME_SCREEN);
        cardContainer.add(new RegisterPanel(this, registrationService), REGISTER_SCREEN);
        cardContainer.add(new CertLoginPanel(this, authService), CERT_LOGIN_SCREEN);

        showScreen(WELCOME_SCREEN);
        setVisible(true);
    }

    public void showScreen(String key) {
        cardLayout.show(cardContainer, key);
    }

    public void showPassLogin(LoginSession session) {
        cardContainer.add(new PassLoginPanel(this, authService, session), PASS_LOGIN_SCREEN);
        showScreen(PASS_LOGIN_SCREEN);
    }

    public void onLoginSuccess(User user) {
        if (user instanceof Organizer organizer) {
            cardContainer.add(new OrganizerScreen(this, organizer, pollService, reportService), ORGANIZER_SCREEN);
            showScreen(ORGANIZER_SCREEN);
        } else if (user instanceof Voter voter) {
            cardContainer.add(new VoterScreen(this, voter, pollService, votingService), VOTER_SCREEN);
            showScreen(VOTER_SCREEN);
        }
    }

    public void logout() {
        showScreen(WELCOME_SCREEN);
    }
}
