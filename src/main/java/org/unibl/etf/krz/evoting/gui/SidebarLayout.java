package org.unibl.etf.krz.evoting.gui;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SidebarLayout extends JPanel {

    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);
    private final JPanel navPanel = new JPanel();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    private String activeKey = null;

    public SidebarLayout() {

        setLayout(new BorderLayout());
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setPreferredSize(new Dimension(220, 0));

        add(navPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }

    public void addNavItem(String key, String label, JPanel content) {

        JButton btn = new JButton(label);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btn.addActionListener(e -> activate(key));

        navButtons.put(key, btn);
        navPanel.add(btn);
        contentPanel.add(content, key);

        if (activeKey == null) {
            activeKey = key;
        }
    }

    public void addBottomSpace() {
        navPanel.add(Box.createVerticalGlue());
    }

    public void addLogoutItem(String label, Runnable onLogout) {
        JButton btn = new JButton(label);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btn.addActionListener(e -> onLogout.run());
        navPanel.add(btn);
    }

    public void showInitial() {
        if (activeKey != null) {
            activate(activeKey);
        }
    }

    private void activate(String key) {
        activeKey = key;
        contentLayout.show(contentPanel, key);
    }
}
