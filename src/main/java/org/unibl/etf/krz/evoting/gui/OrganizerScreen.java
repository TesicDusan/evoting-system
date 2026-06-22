package org.unibl.etf.krz.evoting.gui;

import org.unibl.etf.krz.evoting.crypto.CryptoUtil;
import org.unibl.etf.krz.evoting.model.Organizer;
import org.unibl.etf.krz.evoting.model.Poll;
import org.unibl.etf.krz.evoting.model.PollReport;
import org.unibl.etf.krz.evoting.model.Vote;
import org.unibl.etf.krz.evoting.service.PollService;
import org.unibl.etf.krz.evoting.service.ReportService;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OrganizerScreen extends JPanel {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final MainFrame mainFrame;
    private final Organizer organizer;
    private final PollService pollService;
    private final ReportService reportService;

    private JPanel pollsListPanel;

    public OrganizerScreen(MainFrame mainFrame, Organizer organizer, PollService pollService, ReportService reportService) {
        this.mainFrame = mainFrame;
        this.organizer = organizer;
        this.pollService = pollService;
        this.reportService = reportService;

        setLayout(new BorderLayout());

        SidebarLayout sidebar = new SidebarLayout();
        sidebar.addNavItem("create", "Create poll", buildCreatePanel());
        sidebar.addNavItem("polls", "Polls", buildPollsPanel());
        sidebar.addNavItem("count", "Count votes", buildCountPanel());
        sidebar.addBottomSpace();
        sidebar.addLogoutItem("Logout", mainFrame::logout);
        sidebar.showInitial();

        add(sidebar, BorderLayout.CENTER);
    }

    private JPanel buildCreatePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField nameField = new JTextField();
        JTextField descriptionField = new JTextField();
        JTextField startField = new JTextField();
        JTextField endField = new JTextField();

        startField.setText(LocalDateTime.now().plusHours(1).format(FORMATTER));
        endField.setText(LocalDateTime.now().plusDays(1).plusHours(1).format(FORMATTER));

        addLabeledFiled(panel, "Name", nameField);
        addLabeledFiled(panel, "Description", descriptionField);
        addLabeledFiled(panel, "Start time (dd.MM.yyyy HH:mm)", startField);
        addLabeledFiled(panel, "End time (dd.MM.yyyy HH:mm)", endField);

        panel.add(new JLabel("Options (2-5)"));
        panel.add(Box.createVerticalStrut(5));

        JPanel optionsContainer = new JPanel();
        optionsContainer.setLayout(new BoxLayout(optionsContainer, BoxLayout.Y_AXIS));
        optionsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        List<JTextField> optionFields = new ArrayList<>();
        Runnable addOptionField = () -> {
            if (optionFields.size() >= 5) return;
            JTextField field = new JTextField();
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
            field.setAlignmentX(Component.LEFT_ALIGNMENT);
            optionFields.add(field);
            optionsContainer.add(field);
            optionsContainer.add(Box.createVerticalStrut(5));
            optionsContainer.revalidate();
        };
        addOptionField.run();
        addOptionField.run();

        JButton addOptionBtn = new JButton("Add option");
        addOptionBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addOptionBtn.addActionListener(e -> addOptionField.run());

        panel.add(optionsContainer);
        panel.add(addOptionBtn);
        panel.add(Box.createVerticalStrut(15));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(10));

        JButton createBtn = new JButton("Create poll");
        createBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        createBtn.addActionListener(e -> {
            try {
                LocalDateTime start = LocalDateTime.parse(startField.getText().trim(), FORMATTER);
                LocalDateTime end = LocalDateTime.parse(endField.getText().trim(), FORMATTER);

                List<String> options = new ArrayList<>();
                for (JTextField option : optionFields) {
                    String value = option.getText().trim();
                    if (!value.isEmpty()) options.add(value);
                }

                Poll poll = pollService.createPoll(organizer, nameField.getText().trim(), descriptionField.getText().trim(), start, end, options);

                statusLabel.setText("Poll created. ID: " + poll.getPollId());
                nameField.setText("");
                descriptionField.setText("");
                optionFields.forEach(field -> field.setText(""));
                refreshPolls();
            } catch (PollService.PollException ex) {
                statusLabel.setText(ex.getMessage());
            }
        });
        panel.add(createBtn);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private void refreshPolls() {
        if (pollsListPanel == null) return;
        pollsListPanel.removeAll();
        try {
            List<Poll> polls = pollService.listOrganizerPolls(organizer);
            if (polls.isEmpty()) {
                pollsListPanel.add(new JLabel("No created polls."));
            } else {
                for (Poll poll : polls) {
                    pollsListPanel.add(buildPollCard(poll));
                    pollsListPanel.add(Box.createVerticalStrut(10));
                }
            }
        } catch (PollService.PollException e) {
            pollsListPanel.add(new JLabel("Error: " + e.getMessage()));
        }
        pollsListPanel.revalidate();
    }

    private JPanel buildPollCard(Poll poll) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        card.add(new JLabel(poll.getName()));
        card.add(Box.createVerticalStrut(4));
        JTextField idField = new JTextField("ID: " + poll.getPollId());
        idField.setEditable(false);
        idField.setAlignmentX(Component.LEFT_ALIGNMENT);
        idField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        card.add(idField);
        poll.refreshStatus();
        card.add(new JLabel("Status: " + poll.getStatus()));
        return card;
    }

    private JPanel buildPollsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshPolls());

        pollsListPanel = new JPanel();
        pollsListPanel.setLayout(new BoxLayout(pollsListPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(pollsListPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);

        panel.add(refreshBtn, BorderLayout.SOUTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::refreshPolls);
        return panel;
    }

    private JPanel buildCountPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField idField = new JTextField();
        addLabeledFiled(panel, "Poll ID", idField);

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(10));

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        outputScroll.setPreferredSize(new Dimension(560, 320));
        outputScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        JButton countBtn = new JButton("Count votes");
        countBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton exportBtn = new JButton("Export to txt");
        exportBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportBtn.setEnabled(false);

        final PollReport[] lastReport = new PollReport[1];

        countBtn.addActionListener(e -> {
            String id = idField.getText().trim();
            if (id.isEmpty()) {
                statusLabel.setText("Input poll ID.");
                return;
            }

            Poll poll;
            try {
                poll = pollService.loadPoll(id);
            } catch (PollService.PollException ex) {
                statusLabel.setText(ex.getMessage());
                return;
            }

            char[] passwordChars = PasswordDialog.prompt(this, "Input private key access password:");
            if (passwordChars == null) return;
            String password = new String(passwordChars);

            PrivateKey privateKey;
            try {
                privateKey = CryptoUtil.loadPrivateKeyFromKeystore(organizer.getUsername(), password, organizer.getKeystorePath());
            } catch (Exception ex) {
                statusLabel.setText("Loading private key not possible. Check password.");
                return;
            }

            try {
                PollReport report = reportService.countVotes(poll, organizer, privateKey);
                outputArea.setText(report.displayForm());
                lastReport[0] = report;
                exportBtn.setEnabled(true);
                statusLabel.setText("Count finished and report signed.");
                refreshPolls();
            } catch (ReportService.ReportException ex) {
                statusLabel.setText(ex.getMessage());
            } finally {
                Arrays.fill(passwordChars, '0');
                privateKey = null;
            }
        });

        exportBtn.addActionListener(e -> {
            if (lastReport[0] == null) return;
            String fileName = "report_" + lastReport[0].getResult().getPollId() + ".txt";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                writer.write(lastReport[0].displayForm());
                statusLabel.setText("Report saved: " + fileName);
            } catch (IOException ex) {
                statusLabel.setText("Error while exporting: " + ex.getMessage());
            }
        });

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.add(countBtn);
        buttonRow.add(exportBtn);
        panel.add(buttonRow);
        panel.add(Box.createVerticalStrut(15));
        panel.add(outputScroll);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private void addLabeledFiled(JPanel container, String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(560, 35));
        container.add(label);
        container.add(Box.createVerticalStrut(5));
        container.add(field);
        container.add(Box.createVerticalStrut(15));
    }
}
