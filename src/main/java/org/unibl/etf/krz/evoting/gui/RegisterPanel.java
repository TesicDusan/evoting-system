package org.unibl.etf.krz.evoting.gui;

import org.unibl.etf.krz.evoting.model.Organizer;
import org.unibl.etf.krz.evoting.model.Voter;
import org.unibl.etf.krz.evoting.service.RegistrationService;

import javax.swing.*;
import java.awt.*;

public class RegisterPanel extends JPanel {

    private final MainFrame mainFrame;
    private final RegistrationService registrationService;

    private final JRadioButton organizerRadio = new JRadioButton("Organizer");
    private final JRadioButton voterRadio = new JRadioButton("Voter");

    private final JTextField usernameField = new JTextField("Username");
    private final JPasswordField passwordField = new JPasswordField("Password");

    private final JTextField orgNameField = new JTextField("Name");

    private final JTextField voterFirstNameField = new JTextField("First name");
    private final JTextField voterLastNameField = new JTextField("Last name");

    private final JPanel organizerFieldsPanel = new JPanel();
    private final JPanel voterFieldsPanel = new JPanel();

    private final JLabel statusLabel = new JLabel(" ");

    public RegisterPanel(MainFrame mainFrame, RegistrationService registrationService) {

        this.mainFrame = mainFrame;
        this.registrationService = registrationService;

        setLayout(new GridBagLayout());
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(460, 560));

        ButtonGroup group = new ButtonGroup();
        group.add(organizerRadio);
        group.add(voterRadio);
        voterRadio.setSelected(true);

        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        typePanel.add(voterRadio);
        typePanel.add(organizerRadio);
        typePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(typePanel);
        card.add(Box.createVerticalStrut(20));

        addField(card, "Username", usernameField);
        addField(card, "Password", passwordField);

        organizerFieldsPanel.setLayout(new BoxLayout(organizerFieldsPanel, BoxLayout.Y_AXIS));
        organizerFieldsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        addField(organizerFieldsPanel, "Organizer name", orgNameField);

        voterFieldsPanel.setLayout(new BoxLayout(voterFieldsPanel, BoxLayout.Y_AXIS));
        voterFieldsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        addField(voterFieldsPanel, "First name", voterFirstNameField);
        addField(voterFieldsPanel, "Last name", voterLastNameField);

        card.add(organizerFieldsPanel);
        card.add(voterFieldsPanel);

        organizerFieldsPanel.setVisible(false);
        voterFieldsPanel.setVisible(true);

        organizerRadio.addActionListener(e -> toggleFields());
        voterRadio.addActionListener(e -> toggleFields());

        card.add(Box.createVerticalStrut(10));

        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(10));

        JButton registerBtn = new JButton("Register");
        registerBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerBtn.addActionListener(e -> register());

        JButton backBtn = new JButton("Back");
        backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        backBtn.addActionListener(e -> mainFrame.showScreen(MainFrame.WELCOME_SCREEN));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.add(registerBtn);
        buttonRow.add(backBtn);
        card.add(buttonRow);

        add(card);
    }

    private void toggleFields() {
        boolean isOrganizer = organizerRadio.isSelected();
        organizerFieldsPanel.setVisible(isOrganizer);
        voterFieldsPanel.setVisible(!isOrganizer);
        revalidate();
    }

    private void addField(JPanel panel, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
        panel.add(field);
        panel.add(Box.createVerticalStrut(15));
    }

    private void register() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        try {
            if (organizerRadio.isSelected()) {
                Organizer organizer = registrationService.registerOrg(username, password, orgNameField.getText().trim());
                statusLabel.setText("Registration successful. Certificate path: " + organizer.getCertPath());
            } else {
                Voter voter =registrationService.registerVoter(username, password, voterFirstNameField.getText().trim(), voterLastNameField.getText().trim());
                statusLabel.setText("Registration successful. Certificate path: " + voter.getCertPath());
            }
            clearFields();
        } catch (RegistrationService.RegistrationException e) {
            statusLabel.setText(e.getMessage());
        }
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        orgNameField.setText("");
        voterFirstNameField.setText("");
        voterLastNameField.setText("");
    }
}
