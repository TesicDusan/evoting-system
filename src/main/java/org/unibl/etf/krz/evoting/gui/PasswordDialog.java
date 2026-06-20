package org.unibl.etf.krz.evoting.gui;

import javax.swing.*;
import java.awt.*;

public class PasswordDialog {

    public static char[] prompt(Component parent, String message) {
        JPasswordField passwordField = new JPasswordField("Password");
        passwordField.setPreferredSize(new Dimension(260, 30));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(message);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(10));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(parent, panel, "Confirm password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            return passwordField.getPassword();
        }
        return null;
    }
}
