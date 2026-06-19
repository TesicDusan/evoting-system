package org.unibl.etf.krz.evoting.gui;

import org.unibl.etf.krz.evoting.model.User;
import org.unibl.etf.krz.evoting.service.AuthService;
import org.unibl.etf.krz.evoting.service.LoginSession;

import javax.swing.*;
import java.awt.*;

public class PassLoginPanel extends JPanel {

    private final MainFrame mainFrame;
    private final AuthService authService;
    private final LoginSession session;

    private final JTextField usernameField = new JTextField("Username");
    private final JPasswordField passwordField = new JPasswordField("Password");
    private final JLabel statusLabel = new JLabel(" ");

    public PassLoginPanel(MainFrame mainFrame, AuthService authService, LoginSession session) {
        this.mainFrame = mainFrame;
        this.authService = authService;
        this.session = session;

        setLayout(new GridBagLayout());
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(460, 340));

        addField(card, "Username", usernameField);
        addField(card, "Password", passwordField);

        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(8));

        JButton loginBtn = new JButton("Login");
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.addActionListener(e -> login());

        JButton backBtn = new JButton("Back");
        backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        backBtn.addActionListener(e -> mainFrame.showScreen(MainFrame.WELCOME_SCREEN));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.add(loginBtn);
        buttonRow.add(backBtn);
        card.add(buttonRow);

        add(card);
    }

    private void addField(JPanel panel, String text, JComponent field) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        panel.add(label);
        panel.add(Box.createVerticalStrut(4));
        panel.add(field);
        panel.add(Box.createVerticalStrut(14));
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        try {
            User user = authService.login(session, username, password);
            mainFrame.onLoginSuccess(user);
        } catch (AuthService.LoginException e) {
            statusLabel.setText(e.getMessage());
        }
    }
}
