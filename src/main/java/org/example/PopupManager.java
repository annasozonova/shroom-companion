package org.example;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

/**
 * Utility class for displaying temporary UI popups.
 */
public class PopupManager {
    /**
     * Shows a popup message with a skin icon above the target component to indicate a new skin unlock.
     */
    public static void showUnlockMessage(Component relativeTo, String color) {
        JLabel textLabel = new JLabel("New skin");
        textLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        textLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 6));

        JLabel iconLabel = new JLabel();
        URL iconURL = ShroomCompanion.class.getClassLoader().getResource("mini_" + color + ".png");
        if (iconURL != null) {
            ImageIcon raw = new ImageIcon(iconURL);
            Image scaled = raw.getImage().getScaledInstance(24, 19, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(scaled));
        }

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
        content.setBackground(new Color(255, 255, 200));
        content.setBorder(new LineBorder(new Color(122, 112, 91), 2));
        content.add(textLabel);
        content.add(iconLabel);

        showPopup(relativeTo, content, (relativeTo.getWidth() - content.getPreferredSize().width) / 2, -content.getPreferredSize().height - 2);
    }

    /**
     * Shows a popup with remaining cooldown time beside the target component.
     */
    public static void showCooldownPopup(Component relativeTo, String message) {
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        label.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(255, 255, 200));
        panel.setBorder(new LineBorder(new Color(122, 112, 91), 2));
        panel.add(label, BorderLayout.CENTER);

        showPopup(relativeTo, panel, -panel.getPreferredSize().width - 8, (relativeTo.getHeight() - panel.getPreferredSize().height) / 2);
    }

    /**
     * Displays the given component as a popup near the target with custom offset.
     */
    private static void showPopup(Component relativeTo, JComponent content, int offsetX, int offsetY) {
        JWindow popup = new JWindow();
        popup.setBackground(new Color(0, 0, 0, 0));
        popup.add(content);
        popup.pack();

        Point loc = relativeTo.getLocationOnScreen();
        int x = loc.x + offsetX;
        int y = loc.y + offsetY;

        popup.setLocation(x, y);
        popup.setVisible(true);

        popup.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                popup.setVisible(false);
                popup.dispose();
            }
        });

        new Timer(2000, e -> {
            popup.setVisible(false);
            popup.dispose();
        }).start();
    }
}

