package org.example;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;

public class ClickCharacterApp {
    private static final Preferences prefs =
            Preferences.userRoot().node(ClickCharacterApp.class.getName());
    private static final String CLICK_KEY = "clickCount";

    static class PixelLabel extends JLabel {
        PixelLabel(String text) {
            super(text, SwingConstants.LEFT);
            setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    private static List<ImageIcon> currentFrames = new ArrayList<>();
    private static List<ImageIcon> idleFrames = new ArrayList<>();
    private static List<ImageIcon> walkFrames = new ArrayList<>();

    private static int currentFrameIndex = 0;
    private static Timer animationTimer;
    private static boolean isLooping = false;

    private static int clickCount = 0;
    private static JLabel counterLabel;
    private static JLabel characterLabel;

    private static final int DEFAULT_DELAY = 150;
    private static int walkDelay = DEFAULT_DELAY;
    private static int idleDelay = DEFAULT_DELAY;

    private static final int MIN_WALK_DELAY = 50;
    private static final int DELAY_STEP = 2;

    private static Timer delayResetTimer;
    private static Timer slowDownTimer;

    public static void main(String[] args) {
        clickCount = prefs.getInt(CLICK_KEY, 0);
        SwingUtilities.invokeLater(() -> createAndShowGUI());
        setupGlobalListeners();
    }

    private static void createAndShowGUI() {
        final Point[] mouseDownCompCoords = { null };

        JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.setBackground(new Color(0,0,0,0));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        idleFrames = loadSpriteStrip("cute mushroom idle without white cropped.png", 9,  96);
        walkFrames = loadSpriteStrip("cute mushroom walk without white cropped.png", 4,  96);
        currentFrames = idleFrames;

        characterLabel = new JLabel(currentFrames.get(0), SwingConstants.CENTER);
        characterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension charSize = new Dimension(96,96);
        characterLabel.setPreferredSize(charSize);
        characterLabel.setMaximumSize(charSize);

        Border darkBorder = BorderFactory.createLineBorder(new Color(122,112,91), 2);

        JPanel counterPanel = new JPanel();
        counterPanel.setPreferredSize(new Dimension(70, 25));
        counterPanel.setMaximumSize(counterPanel.getPreferredSize());
        counterPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        counterPanel.setBackground( new Color(238,219,177));
        counterPanel.setBorder(darkBorder);
        counterPanel.setLayout(new BorderLayout());
        counterPanel.setOpaque(true);

        counterLabel = new PixelLabel("");
        counterLabel.setText(String.valueOf(clickCount));
        counterLabel.setBorder(new EmptyBorder(0,8,0,0));

        counterPanel.add(counterLabel);

        content.add(characterLabel);
        content.add(counterPanel);

        frame.setContentPane(content);
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

        ensureAnimationLoop(idleFrames, true, 100);
    }

    private static List<ImageIcon> loadSpriteStrip(String fileName, int frameCount, int scaleWidth) {
        List<ImageIcon> frames = new ArrayList<>();
        URL resource = ClickCharacterApp.class.getClassLoader().getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException("Не найден файл: " + fileName);
        }

        ImageIcon sheetIcon = new ImageIcon(resource);
        Image sheet = sheetIcon.getImage();

        int sheetW = sheetIcon.getIconWidth();
        int sheetH = sheetIcon.getIconHeight();
        int frameW = sheetW / frameCount;
        int frameH = sheetH;

        int scaleHeight = (int)(long)frameH * scaleWidth / frameW;

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

    private static Image createSubImage(Image source, int x, int y, int width, int height) {
        ImageIcon icon = new ImageIcon(source);
        BufferedImage buffered = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = buffered.createGraphics();
        g.drawImage(source,0,0,null);
        g.dispose();
        return buffered.getSubimage(x, y, width, height);
    }

    private static void ensureAnimationLoop(List<ImageIcon> frames, boolean loop, int delay){
        if (animationTimer != null && animationTimer.isRunning() && currentFrames == frames){
            animationTimer.setDelay(delay);
            return;
        }

        if (animationTimer != null) animationTimer.stop();

        currentFrames = frames;
        currentFrameIndex = 0;
        isLooping = loop;

        animationTimer = new Timer(delay, null);
        animationTimer.addActionListener(e -> {
            if (currentFrameIndex >= currentFrames.size()){
                if (isLooping) currentFrameIndex = 0;
                else {
                    animationTimer.stop();
                    return;
                }
            }
            characterLabel.setIcon(currentFrames.get(currentFrameIndex));
            currentFrameIndex++;
        });

        animationTimer.start();
    }

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
            public void nativeMousePressed(NativeMouseEvent e) {}

            @Override
            public void nativeMouseReleased(NativeMouseEvent e) {}
        });

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
                //Для подсчета нажатий при зажатых клавишах
                //handleGlobalClick();
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
                handleGlobalClick();
            }

            @Override
            public void nativeKeyTyped(NativeKeyEvent nativeEvent) {}
        });
    }

    private static void handleGlobalClick() {
        clickCount++;
        prefs.putInt(CLICK_KEY, clickCount);

        SwingUtilities.invokeLater(() -> {
            counterLabel.setText(String.valueOf(clickCount));

            walkDelay = Math.max(MIN_WALK_DELAY, walkDelay - DELAY_STEP);
            ensureAnimationLoop(walkFrames, true, walkDelay);

            if (delayResetTimer != null && delayResetTimer.isRunning())
                delayResetTimer.stop();

            delayResetTimer = new Timer(300, e -> {
                idleDelay = walkDelay;
                walkDelay = DEFAULT_DELAY;
                ensureAnimationLoop(idleFrames, true, idleDelay);

                if (slowDownTimer != null && slowDownTimer.isRunning()) slowDownTimer.stop();

                slowDownTimer = new Timer(500, ev -> {
                   idleDelay = Math.min(idleDelay + 10, DEFAULT_DELAY);
                   animationTimer.setDelay(idleDelay);
                   if (idleDelay == DEFAULT_DELAY) slowDownTimer.stop();
                });
                slowDownTimer.start();
            });

            delayResetTimer.setRepeats(false);
            delayResetTimer.start();
        });
    }
}
