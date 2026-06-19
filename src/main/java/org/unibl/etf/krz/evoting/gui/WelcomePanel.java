package org.unibl.etf.krz.evoting.gui;

import javax.swing.*;
import java.awt.*;

public class WelcomePanel extends JPanel {

    public WelcomePanel(MainFrame mainFrame) {
        setLayout(new GridBagLayout());

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JButton loginBtn = new JButton("Login");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(260, 42));
        loginBtn.addActionListener(e -> mainFrame.showScreen(MainFrame.CERT_LOGIN_SCREEN));

        JButton registerBtn = new JButton("Register");
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.setMaximumSize(new Dimension(260, 42));
        registerBtn.addActionListener(e -> mainFrame.showScreen(MainFrame.REGISTER_SCREEN));

        card.add(loginBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(registerBtn);

        add(card);
    }
}
