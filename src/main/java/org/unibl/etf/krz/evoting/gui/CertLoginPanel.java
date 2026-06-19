package org.unibl.etf.krz.evoting.gui;

import org.unibl.etf.krz.evoting.service.AuthService;
import org.unibl.etf.krz.evoting.service.LoginSession;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class CertLoginPanel extends JPanel {

    private final MainFrame mainFrame;
    private final AuthService authService;

    private final JTextField pathField = new JTextField("Certificate path");
    private final JLabel statusLabel = new JLabel(" ");

    public CertLoginPanel(MainFrame mainFrame, AuthService authService) {
        this.mainFrame = mainFrame;
        this.authService = authService;

        setLayout(new GridBagLayout());
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(460, 280));

        JLabel sub = new JLabel("Choose certificate");
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sub);

        JPanel pathRow = new JPanel(new BorderLayout(8, 0));
        pathRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        pathRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        pathField.setMinimumSize(new Dimension(Integer.MAX_VALUE, 34));
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(e -> browse());
        pathRow.add(pathField, BorderLayout.CENTER);
        pathRow.add(browseBtn, BorderLayout.EAST);
        card.add(pathRow);

        card.add(Box.createVerticalStrut(14));

        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(8));

        JButton validateBtn = new JButton("Validate certificate");
        validateBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        validateBtn.addActionListener(e -> validateCert());

        JButton backBtn = new JButton("Back");
        backBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        validateBtn.addActionListener(e -> mainFrame.showScreen(MainFrame.WELCOME_SCREEN));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.add(validateBtn);
        buttonRow.add(backBtn);
        card.add(buttonRow);

        add(card);
    }

    private void browse() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Certificate (*.cer)", "cer"));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            pathField.setText(f.getAbsolutePath());
        }
    }

    private void validateCert() {
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            statusLabel.setText("No file chosen");
            return;
        }
        try {
            LoginSession session = authService.validateCertificate(path);
            statusLabel.setText("Certificate valid");
            mainFrame.showPassLogin(session);
            pathField.setText("");
            statusLabel.setText(" ");
        } catch (AuthService.LoginException e) {
            statusLabel.setText(e.getMessage());
        }
    }
}
