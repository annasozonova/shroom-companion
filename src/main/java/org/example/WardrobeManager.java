package org.example;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.net.URL;

/**
 * Manages the wardrobe panel and skin selection buttons.
 * Provides functionality to toggle, populate, and interact with the wardrobe UI.
 */
public class WardrobeManager {
    private static JPanel wardrobePanel;
    private static JScrollPane wardrobeScrollPane;

    /**
     * Creates the wardrobe toggle button with an icon and click handler.
     */
    public static JButton createWardrobeButton(JFrame frame) {
        JButton wardrobeButton = new JButton();
        URL iconURL = ClickCharacterApp.class.getResource("/hanger.png");
        ImageIcon wardrobeIconRaw = new ImageIcon(iconURL);
        Image wardrobeIcon = wardrobeIconRaw.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
        wardrobeButton.setIcon(new ImageIcon(wardrobeIcon));
        wardrobeButton.setMargin(new Insets(0, 0, 0, 2));
        wardrobeButton.setFocusable(false);
        wardrobeButton.setBorderPainted(false);
        wardrobeButton.setContentAreaFilled(false);
        wardrobeButton.setFocusPainted(false);
        wardrobeButton.setOpaque(false);

        wardrobeButton.addActionListener(e -> {
            wardrobeScrollPane.setVisible(!wardrobeScrollPane.isVisible());
            frame.pack();
        });

        return wardrobeButton;
    }

    /**
     * Creates and configures the scrollable wardrobe panel with all unlocked skins.
     */
    public static JScrollPane createWardrobeScrollPane(Border darkBorder) {
        wardrobePanel = new JPanel(new GridLayout(0, 1, 0, 2));
        wardrobePanel.setBackground(new Color(238, 219, 177));

        for (String color : SkinManager.getUnlockedColors()) {
            addColorToWardrobe(color);
        }

        wardrobeScrollPane = new JScrollPane(wardrobePanel);
        wardrobeScrollPane.setBorder(darkBorder);
        wardrobeScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        wardrobeScrollPane.setPreferredSize(new Dimension(100, 130));
        wardrobeScrollPane.setVisible(false);

        JScrollBar vertical = wardrobeScrollPane.getVerticalScrollBar();
        vertical.setUnitIncrement(10);
        vertical.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(122, 112, 91);
                this.trackColor = new Color(238, 219, 177);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
        vertical.setPreferredSize(new Dimension(6, 0));

        return wardrobeScrollPane;
    }

    /**
     * Adds a skin button to the wardrobe and updates the panel.
     */
    public static void addColorToWardrobe(String color) {
        String filename = "mini_" + color + ".png";
        URL miniIconURL = ClickCharacterApp.class.getClassLoader().getResource(filename);
        if (miniIconURL == null) {
            System.out.println("Icon for mini not found: " + filename);
            return;
        }

        ImageIcon miniIconRaw = new ImageIcon(miniIconURL);
        JButton colorBtn = new JButton(new ImageIcon(miniIconRaw.getImage().getScaledInstance(48, 38, Image.SCALE_SMOOTH)));
        colorBtn.setFocusPainted(false);
        colorBtn.setContentAreaFilled(false);
        colorBtn.setOpaque(false);
        colorBtn.setBorderPainted(false);
        colorBtn.addActionListener(e -> {
            SkinManager.setSelectedColor(color);
            AnimationManager.setCurrentFrames(ClickCharacterApp.getIdleSprites().get(color));
            AnimationManager.playIdle();
        });

        wardrobePanel.add(colorBtn);
        wardrobePanel.revalidate();
        wardrobePanel.repaint();
    }

    /**
     * Returns whether the wardrobe panel is currently open.
     */
    public static boolean isWardrobeOpen() {
        return wardrobeScrollPane != null && wardrobeScrollPane.isVisible();
    }

    /**
     * Hides the wardrobe panel if it is visible.
     */
    public static void closeWardrobe() {
        if (wardrobeScrollPane != null) {
            wardrobeScrollPane.setVisible(false);
        }
    }
}
