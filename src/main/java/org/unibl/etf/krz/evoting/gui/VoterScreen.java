package org.unibl.etf.krz.evoting.gui;

import org.unibl.etf.krz.evoting.crypto.CryptoUtil;
import org.unibl.etf.krz.evoting.model.Poll;
import org.unibl.etf.krz.evoting.model.Voter;
import org.unibl.etf.krz.evoting.service.PollService;
import org.unibl.etf.krz.evoting.service.VotingService;
import org.unibl.etf.krz.evoting.storage.DataStore;

import javax.swing.*;
import java.awt.*;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VoterScreen extends JPanel {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final MainFrame mainFrame;
    private final Voter voter;
    private final PollService pollService;
    private final VotingService votingService;

    private JPanel pollsListPanel;
    private JPanel receiptsListPanel;

    public VoterScreen(MainFrame mainFrame, Voter voter, PollService pollService, VotingService votingService) {
        this.mainFrame = mainFrame;
        this.voter = voter;
        this.pollService = pollService;
        this.votingService = votingService;

        setLayout(new BorderLayout());

        SidebarLayout sidebar = new SidebarLayout();

        sidebar.addNavItem("polls", "Polls", buildPollsPanel());
        sidebar.addNavItem("receipts", "Receipts", buildReceiptsPanel());
        sidebar.addBottomSpace();
        sidebar.addLogoutItem("Logout", mainFrame::logout);
        sidebar.showInitial();

        add(sidebar, BorderLayout.CENTER);
    }

    private JPanel buildPollsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshPolls());

        pollsListPanel = new JPanel();
        pollsListPanel.setLayout(new BoxLayout(pollsListPanel, BoxLayout.Y_AXIS));

        panel.add(new JScrollPane(pollsListPanel), BorderLayout.CENTER);
        panel.add(refreshBtn, BorderLayout.SOUTH);

        refreshPolls();
        return panel;
    }

    private void refreshPolls() {
        if (pollsListPanel == null) return;
        pollsListPanel.removeAll();
        try {
            List<Poll> polls = pollService.listActivePolls();
            if (polls.isEmpty()) {
                pollsListPanel.add(new JLabel("No active polls."));
            } else {
                for (Poll poll : polls) {
                    pollsListPanel.add(buildPollCard(poll));
                    pollsListPanel.add(Box.createVerticalStrut(15));
                }
            }
        } catch (PollService.PollException e) {
            pollsListPanel.add(new JLabel("Error: " + e.getMessage()));
        }
        pollsListPanel.revalidate();
    }

    private JPanel buildPollCard(Poll poll) {
        boolean hasVoted = voter.votedIn(poll.getPollId());

        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(poll.getName());
        infoPanel.add(name);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(new JLabel(poll.getDescription()));
        infoPanel.add(new JLabel("Start: " + poll.getStartTime().format(FORMATTER)));
        infoPanel.add(new JLabel("End: " + poll.getEndTime().format(FORMATTER)));
        infoPanel.add(new JLabel("Options: " + String.join(", ", poll.getOptions())));

        JPanel votePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        if (hasVoted) {
            votePanel.add(new JLabel("Already voted."));
        } else {
            JButton voteBtn = new JButton("Vote");
            voteBtn.addActionListener(e -> openVoteDialog(poll));
            votePanel.add(voteBtn);
        }

        card.add(infoPanel, BorderLayout.CENTER);
        card.add(votePanel, BorderLayout.EAST);
        return  card;
    }

    private void openVoteDialog(Poll poll) {
        List<String> options = poll.getOptions();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(new JLabel(poll.getName()));
        panel.add(Box.createVerticalStrut(15));
        panel.add(new JLabel("Choose option:"));
        panel.add(Box.createVerticalStrut(5));

        ButtonGroup btnGroup = new ButtonGroup();
        List<JRadioButton> radioButtons = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            JRadioButton btn = new JRadioButton(options.get(i));
            btn.setActionCommand(String.valueOf(i));
            btnGroup.add(btn);
            radioButtons.add(btn);
            panel.add(btn);
        }

        int result = JOptionPane.showConfirmDialog(this, panel, "Poll: " + poll.getName(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String selectCommand = null;
        for (JRadioButton btn : radioButtons) {
            if (btn.isSelected()) {
                selectCommand = btn.getActionCommand();
                break;
            }
        }
        if (selectCommand == null) {
            JOptionPane.showMessageDialog(this, "No option selected.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final int selectedId = Integer.parseInt(selectCommand);

        char[] passwordChars = PasswordDialog.prompt(this, "Input private key access password:");
        if (passwordChars == null) return;
        String password = new String(passwordChars);

        PrivateKey privateKey;
        try {
            privateKey = CryptoUtil.loadPrivateKeyFromKeystore(voter.getUsername(), password, voter.getKeystorePath());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Loading private key not possible. Check password.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            X509Certificate orgCert = CryptoUtil.loadCertificate(DataStore.getCertificatePath(poll.getOrgUsername()));
            String receipt = votingService.vote(poll, voter, selectedId, orgCert, privateKey);

            JOptionPane.showMessageDialog(this, "Vote recorded successfully.");
            refreshPolls();
            refreshReceipts();
        } catch (VotingService.VotingException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(passwordChars, '0');
            privateKey = null;
        }
    }

    private JPanel buildReceiptsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshReceipts());

        receiptsListPanel = new JPanel();
        receiptsListPanel.setLayout(new BoxLayout(receiptsListPanel, BoxLayout.Y_AXIS));

        panel.add(new JScrollPane(receiptsListPanel), BorderLayout.CENTER);
        panel.add(refreshBtn, BorderLayout.SOUTH);

        refreshReceipts();
        return panel;
    }

    private void refreshReceipts() {
        if (receiptsListPanel == null) return;
        receiptsListPanel.removeAll();
        Map<String, String> polls = voter.getVotedInPolls();
        if (polls.isEmpty()) {
            receiptsListPanel.add(new JLabel("No recorded votes"));
        } else {
            polls.forEach((pollId, receiptHash) -> {
                receiptsListPanel.add(buildReceiptCard(pollId, receiptHash));
                receiptsListPanel.add(Box.createVerticalStrut(10));
            });
        }
        receiptsListPanel.revalidate();
    }

    private JPanel buildReceiptCard(String pollId, String receiptHash) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.add(new JLabel("Poll ID: " + pollId));
        infoPanel.add(Box.createVerticalStrut(5));
        JTextField receiptField = new JTextField(receiptHash);
        receiptField.setEditable(false);
        infoPanel.add(receiptField);

        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));

        JLabel resultLabel = new JLabel(" ");
        resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton verifyBtn = new JButton("Verify");
        verifyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        verifyBtn.addActionListener(e -> {
            try {
                Poll poll = pollService.loadPoll(pollId);
                X509Certificate voterCert = CryptoUtil.loadCertificate(voter.getCertPath());
                boolean valid = votingService.verifyVote(poll, voter, receiptHash, voterCert);
                if (valid) {
                    resultLabel.setText("Vote valid");
                } else {
                    resultLabel.setText("Vote not valid");
                }
            } catch (PollService.PollException ex) {
                resultLabel.setText("Error while loading poll: " + ex.getMessage());
            } catch (Exception ex) {
                resultLabel.setText("Error: " + ex.getMessage());
            }
        });

        actionPanel.add(verifyBtn);
        actionPanel.add(Box.createVerticalStrut(5));
        actionPanel.add(resultLabel);

        card.add(infoPanel, BorderLayout.CENTER);
        card.add(actionPanel, BorderLayout.EAST);
        return card;
    }
}
