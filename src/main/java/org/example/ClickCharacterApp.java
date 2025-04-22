package org.example;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;

/**
 * Main class of the application.
 * Handles initialization, UI setup, global input listeners, and main animation logic.
 */
public class ClickCharacterApp {
    private static final Map<String, List<ImageIcon>> idleSprites = new HashMap<>();
    private static final Map<String, List<ImageIcon>> walkSprites = new HashMap<>();

    private static JLabel counterLabel;

    /**
     * Custom label class with pixelated monospaced font rendering.
     */
    static class PixelLabel extends JLabel {
        PixelLabel(String text) {
            super(text, SwingConstants.LEFT);
            setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    /**
     * Entry point: loads progress, sets up GUI and global input.
     */
    public static void main(String[] args) {
        SkinManager.loadProgress();
        SwingUtilities.invokeLater(() -> createAndShowGUI());
        setupGlobalListeners();
    }

    public static Map<String, List<ImageIcon>> getIdleSprites() {
        return idleSprites;
    }

    /**
     * Initializes and displays the main UI window and components.
     */
    private static void createAndShowGUI() {
        final Point[] mouseDownCompCoords = {null};

        JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.setBackground(new Color(0, 0, 0, 0));

        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.X_AXIS));
        rootPanel.setOpaque(false);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setPreferredSize(new Dimension(96, 130));
        leftPanel.setMaximumSize(new Dimension(96, 130));
        leftPanel.setOpaque(false);

        String[] colors = {"red", "blue", "green", "light_blue", "orange", "pink", "violet", "yellow"};
        for (String color : colors) {
            idleSprites.put(color, loadSpriteStrip("idle_" + color + ".png", 9, 96));
            walkSprites.put(color, loadSpriteStrip("walk_" + color + ".png", 4, 96));
        }

        List<ImageIcon> currentFrames = idleSprites.get(SkinManager.getSelectedColor());

        JLabel characterLabel = new JLabel(currentFrames.get(0), SwingConstants.CENTER);
        characterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension charSize = new Dimension(96, 96);
        characterLabel.setPreferredSize(charSize);
        characterLabel.setMaximumSize(charSize);
        AnimationManager.setCharacterLabel(characterLabel);
        AnimationManager.setCurrentFrames(currentFrames);
        AnimationManager.playIdle();

        Border darkBorder = BorderFactory.createLineBorder(new Color(122, 112, 91), 2);

        JPanel counterPanel = new JPanel();
        counterPanel.setPreferredSize(new Dimension(80, 25));
        counterPanel.setMaximumSize(counterPanel.getPreferredSize());
        counterPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        counterPanel.setBackground(new Color(238, 219, 177));
        counterPanel.setBorder(darkBorder);
        counterPanel.setLayout(new BorderLayout());
        counterPanel.setOpaque(true);

        counterLabel = new PixelLabel(String.valueOf(SkinManager.getClickCount()));
        counterLabel.setBorder(new EmptyBorder(0, 6, 0, 0));

        JButton wardrobeButton = WardrobeManager.createWardrobeButton(frame);
        counterPanel.add(counterLabel);
        counterPanel.add(wardrobeButton, BorderLayout.EAST);

        leftPanel.add(Box.createVerticalGlue());
        leftPanel.add(characterLabel);
        leftPanel.add(counterPanel);

        JScrollPane wardrobeScrollPane = WardrobeManager.createWardrobeScrollPane(darkBorder);

        rootPanel.add(leftPanel);
        rootPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        rootPanel.add(wardrobeScrollPane);

        frame.setContentPane(rootPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });

        frame.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currCords = e.getLocationOnScreen();
                frame.setLocation(
                        currCords.x - mouseDownCompCoords[0].x,
                        currCords.y - mouseDownCompCoords[0].y
                );
            }
        });

        counterLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                long minutes = SkinManager.getRemainingCooldownMinutes();
                String text = (minutes == 0) ? "0 min" : minutes + " min";
                PopupManager.showCooldownPopup(counterLabel, text);
            }
        });

        AnimationManager.playIdle();
    }

    /**
     * Sets up global mouse and keyboard listeners using JNativeHook.
     */
    private static void setupGlobalListeners() {
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
            return;
        }

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);

        GlobalScreen.addNativeMouseListener(new NativeMouseListener() {
            @Override
            public void nativeMouseClicked(NativeMouseEvent e) {
                handleGlobalClick();
            }

            @Override
            public void nativeMousePressed(NativeMouseEvent e) {
            }

            @Override
            public void nativeMouseReleased(NativeMouseEvent e) {
            }
        });

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                //Debug functions
                /*
                if (e.getKeyCode() == NativeKeyEvent.VC_U && (e.getModifiers() & e.CTRL_MASK) != 0) {
                    unlockSkinForDebug();
                }*/
                /*else {
                    handleGlobalClick();
                }
                */
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
                handleGlobalClick();
            }

            @Override
            public void nativeKeyTyped(NativeKeyEvent nativeEvent) {
            }
        });
    }

    /**
     * Handles click events globally: updates counter, animation, unlock logic.
     */
    private static void handleGlobalClick() {
        if (WardrobeManager.isWardrobeOpen()) {
            PointerInfo info = MouseInfo.getPointerInfo();
            Point mouseLoc = info.getLocation();

            Window window = SwingUtilities.getWindowAncestor(counterLabel);
            if (window != null && !window.getBounds().contains(mouseLoc)) {
                SwingUtilities.invokeLater(WardrobeManager::closeWardrobe);
                return;
            }
        }

        SkinManager.incrementClickCount();

        SwingUtilities.invokeLater(() -> {
            counterLabel.setText(String.valueOf(SkinManager.getClickCount()));

            List<ImageIcon> walkFrames = walkSprites.get(SkinManager.getSelectedColor());
            AnimationManager.playWalk(walkFrames,() -> {
                if (SkinManager.tryUnlockNewSkin()){
                    String newColor = SkinManager.getLastUnlockedColor();
                    WardrobeManager.addColorToWardrobe(newColor);
                    PopupManager.showUnlockMessage(AnimationManager.getCharacterLabel(), newColor);
                }
            });
        });
    }

    /**
     * Loads a horizontal sprite strip and splits it into individual frames.
     */
    private static List<ImageIcon> loadSpriteStrip(String fileName, int frameCount, int scaleWidth) {
        List<ImageIcon> frames = new ArrayList<>();
        URL resource = ClickCharacterApp.class.getClassLoader().getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException("File not found: " + fileName);
        }

        ImageIcon sheetIcon = new ImageIcon(resource);
        Image sheet = sheetIcon.getImage();

        int sheetW = sheetIcon.getIconWidth();
        int sheetH = sheetIcon.getIconHeight();
        int frameW = sheetW / frameCount;
        int frameH = sheetH;

        int scaleHeight = (int) (long) frameH * scaleWidth / frameW;

        for (int i = 0; i < frameCount; i++) {
            Image frame = createSubImage(sheet,
                    i * frameW, 0,
                    frameW, frameH);
            Image scaled = frame.getScaledInstance(
                    scaleWidth, scaleHeight,
                    Image.SCALE_SMOOTH);
            frames.add(new ImageIcon(scaled));
        }
        return frames;
    }

    /**
     * Extracts a sub-image from a sprite sheet.
     */
    private static Image createSubImage(Image source, int x, int y, int width, int height) {
        ImageIcon icon = new ImageIcon(source);
        BufferedImage buffered = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = buffered.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return buffered.getSubimage(x, y, width, height);
    }

    /**
     * Unlocks a skin immediately for testing (debug).
     */
    private static void unlockSkinForDebug() {
        try {
            for (int i = SkinManager.getClickCount(); i < 1000; i++) {
                SkinManager.incrementClickCount();
            }
            Preferences prefs = Preferences.userRoot().node(ClickCharacterApp.class.getName());
            prefs.putLong("lastUnlockTime", System.currentTimeMillis() - 999_999);

            if (SkinManager.tryUnlockNewSkin()) {
                String newColor = SkinManager.getLastUnlockedColor();
                WardrobeManager.addColorToWardrobe(newColor);
                PopupManager.showUnlockMessage(AnimationManager.getCharacterLabel(), newColor);
            } else {
                System.out.println("No skins available to unlock");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
