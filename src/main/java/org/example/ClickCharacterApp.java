package org.example;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
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

public class ClickCharacterApp {
    private static final Preferences prefs =
            Preferences.userRoot().node(ClickCharacterApp.class.getName());
    private static final String CLICK_KEY = "clickCount";
    private static final String COLOR_KEY = "selectedColor";

    static class PixelLabel extends JLabel {
        PixelLabel(String text) {
            super(text, SwingConstants.LEFT);
            setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    private static Map<String, List<ImageIcon>> idleSprites = new HashMap<>();
    private static Map<String, List<ImageIcon>> walkSprites = new HashMap<>();
    private static List<ImageIcon> currentFrames = new ArrayList<>();

    private static String selectedColor;

    private static int currentFrameIndex = 0;
    private static Timer animationTimer;
    private static boolean isLooping = false;

    private static int clickCount = 0;
    private static JLabel counterLabel;
    private static JLabel characterLabel;
    private static JPanel wardrobePanel;
    private static JScrollPane wardrobeScrollPane;

    private static final int DEFAULT_DELAY = 150;
    private static int walkDelay = DEFAULT_DELAY;
    private static int idleDelay = DEFAULT_DELAY;

    private static final int MIN_WALK_DELAY = 50;
    private static final int DELAY_STEP = 2;

    private static Timer delayResetTimer;
    private static Timer slowDownTimer;

    private static final String LAST_UNLOCK_KEY = "lastUnlockTime";
    private static final String UNLOCKED_COLORS_KEY = "UnlockedColors";
    private static final long UNLOCK_COOLDOWN_MS = 3600_000;
    private static List<String> unlockedColors = new ArrayList<>();

    private static JLayeredPane layeredPane;
    private static JLabel unlockLabel;

    public static void main(String[] args) {
        loadProgress();
        SwingUtilities.invokeLater(() -> createAndShowGUI());
        setupGlobalListeners();
    }

    private static void loadProgress() {
        clickCount = prefs.getInt(CLICK_KEY, 0);
        selectedColor = prefs.get(COLOR_KEY, "red");

        String saved = prefs.get(UNLOCKED_COLORS_KEY, "red");
        unlockedColors = new ArrayList<>(List.of(saved.split(",")));
    }

    private static void saveUnlockedColors() {
        prefs.put(UNLOCKED_COLORS_KEY, String.join(",", unlockedColors));
    }

    private static void tryUnlockNewSkin() {
        long now = System.currentTimeMillis();
        long lastUnlock = prefs.getLong(LAST_UNLOCK_KEY, 0);

        if (clickCount >= 1000) {
            //if (clickCount >= 1000 && now - lastUnlock >= UNLOCK_COOLDOWN_MS){
            clickCount -= 1000;
            List<String> allColors = List.of("red", "blue", "green", "light_blue", "orange", "pink", "violet", "yellow");
            List<String> locked = new ArrayList<>(allColors);
            locked.removeAll(unlockedColors);

            if (!locked.isEmpty()) {
                String newColor = locked.get((int) (Math.random() * locked.size()));
                unlockedColors.add(newColor);
                saveUnlockedColors();
                prefs.putLong(LAST_UNLOCK_KEY, now);
                addColorToWardrobe(newColor);

                showUnlockMessage(characterLabel, "New skin: " + newColor + "!");
            }
        }
    }

    private static void showUnlockMessage(JComponent target, String text) {
        if (unlockLabel != null) layeredPane.remove(unlockLabel);

        unlockLabel = new JLabel(text, SwingConstants.CENTER);
        unlockLabel.setOpaque(true);
        unlockLabel.setBackground(new Color(255, 255, 200));
        unlockLabel.setBorder(BorderFactory.createLineBorder(new Color(122, 112, 91), 2));
        unlockLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));

        Point loc = SwingUtilities.convertPoint(target.getParent(), target.getLocation(), layeredPane);
        int labelWidth = text.length() * 7 + 20;
        int labelHeight = 25;

        int x = loc.x + (target.getWidth() - labelWidth) / 2;
        int y = loc.y - labelHeight - 5;
        if (y < 0) y = 5;

        unlockLabel.setBounds(x, y, labelWidth, labelHeight);

        unlockLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                layeredPane.remove(unlockLabel);
                layeredPane.repaint();
            }
        });

        layeredPane.add(unlockLabel, JLayeredPane.POPUP_LAYER);
        layeredPane.repaint();
    }

    private static void addColorToWardrobe(String color) {
        String filename = "mini_" + color + ".png";
        URL miniIconURL = ClickCharacterApp.class.getClassLoader().getResource(filename);
        if (miniIconURL != null) {
            ImageIcon miniIconRaw = new ImageIcon(miniIconURL);
            JButton colorBtn = new JButton(new ImageIcon(miniIconRaw.getImage().getScaledInstance(48, 38, Image.SCALE_SMOOTH)));
            colorBtn.setFocusPainted(false);
            colorBtn.setContentAreaFilled(false);
            colorBtn.setOpaque(false);
            colorBtn.setBorderPainted(false);
            colorBtn.addActionListener(e -> {
                selectedColor = color;
                prefs.put(COLOR_KEY, selectedColor);
                currentFrames = idleSprites.get(selectedColor);
                ensureAnimationLoop(currentFrames, true, idleDelay);
            });

            wardrobePanel.add(colorBtn);
            wardrobePanel.revalidate();
            wardrobePanel.repaint();
        } else {
            System.out.println("Icon for mini not found: " + filename);
        }
    }

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

        currentFrames = idleSprites.get(selectedColor);

        characterLabel = new JLabel(currentFrames.get(0), SwingConstants.CENTER);
        characterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension charSize = new Dimension(96, 96);
        characterLabel.setPreferredSize(charSize);
        characterLabel.setMaximumSize(charSize);

        Border darkBorder = BorderFactory.createLineBorder(new Color(122, 112, 91), 2);

        JPanel counterPanel = new JPanel();
        counterPanel.setPreferredSize(new Dimension(80, 25));
        counterPanel.setMaximumSize(counterPanel.getPreferredSize());
        counterPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        counterPanel.setBackground(new Color(238, 219, 177));
        counterPanel.setBorder(darkBorder);
        counterPanel.setLayout(new BorderLayout());
        counterPanel.setOpaque(true);

        counterLabel = new PixelLabel(String.valueOf(clickCount));
        counterLabel.setBorder(new EmptyBorder(0, 4, 0, 0));

        JButton wardrobeButton = new JButton();
        URL iconURL = ClickCharacterApp.class.getResource("/hanger.png"); // если в корне src или resources
        ImageIcon wardrobe_icon_raw = new ImageIcon(iconURL);
        Image wardrobe_icon = wardrobe_icon_raw.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
        wardrobeButton.setIcon(new ImageIcon(wardrobe_icon));
        wardrobeButton.setMargin(new Insets(0, 0, 0, 2));
        wardrobeButton.setFocusable(false);
        wardrobeButton.setBorderPainted(false);
        wardrobeButton.setContentAreaFilled(false);
        wardrobeButton.setFocusPainted(false);
        wardrobeButton.setOpaque(false);
        wardrobeButton.addActionListener(e -> {
            wardrobeScrollPane.setVisible(!wardrobeScrollPane.isVisible());

            Dimension rootSize = rootPanel.getPreferredSize();
            rootPanel.setBounds(0, 0, rootSize.width, rootSize.height);
            layeredPane.setPreferredSize(rootSize);
            layeredPane.revalidate();
            layeredPane.repaint();

            frame.setSize(layeredPane.getPreferredSize());
        });

        counterPanel.add(counterLabel);
        counterPanel.add(wardrobeButton, BorderLayout.EAST);

        leftPanel.add(Box.createVerticalGlue());
        leftPanel.add(characterLabel);
        leftPanel.add(counterPanel);

        wardrobePanel = new JPanel(new GridLayout(0, 1, 4, 2));
        wardrobePanel.setBackground(new Color(238, 219, 177));

        for (String color : unlockedColors) {
            String filename = "mini_" + color + ".png";
            URL miniIconURL = ClickCharacterApp.class.getClassLoader().getResource(filename);
            if (miniIconURL != null) {
                ImageIcon miniIconRaw = new ImageIcon(miniIconURL);
                JButton colorBtn = new JButton(new ImageIcon(miniIconRaw.getImage().getScaledInstance(48, 38, Image.SCALE_SMOOTH)));
                colorBtn.setFocusPainted(false);
                colorBtn.setContentAreaFilled(false);
                colorBtn.setOpaque(false);
                colorBtn.setBorderPainted(false);
                colorBtn.addActionListener(e -> {
                    selectedColor = color;
                    prefs.put(COLOR_KEY, selectedColor);
                    currentFrames = idleSprites.get(selectedColor);
                    ensureAnimationLoop(currentFrames, true, idleDelay);
                });

                wardrobePanel.add(colorBtn);
            } else System.out.println("Icon for mini not found");
        }

        wardrobeScrollPane = new JScrollPane(wardrobePanel);
        wardrobeScrollPane.setBorder(darkBorder);
        wardrobeScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        wardrobeScrollPane.setPreferredSize(new Dimension(100, 130));
        wardrobeScrollPane.setVisible(false);

        JScrollBar vertical = wardrobeScrollPane.getVerticalScrollBar();
        vertical.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(122, 112, 91); // цвет ползунка
                this.trackColor = new Color(238, 219, 177); // фон трека
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
        vertical.setPreferredSize(new Dimension(6, 0)); // ширина скроллбара


        rootPanel.add(leftPanel);
        rootPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        rootPanel.add(wardrobeScrollPane);

        layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);

        rootPanel.setBounds(0, 0, rootPanel.getPreferredSize().width, rootPanel.getPreferredSize().height);
        layeredPane.setPreferredSize(rootPanel.getPreferredSize());
        layeredPane.add(rootPanel, JLayeredPane.DEFAULT_LAYER);

        frame.setContentPane(layeredPane);
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

        ensureAnimationLoop(currentFrames, true, 100);
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

    private static Image createSubImage(Image source, int x, int y, int width, int height) {
        ImageIcon icon = new ImageIcon(source);
        BufferedImage buffered = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = buffered.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return buffered.getSubimage(x, y, width, height);
    }

    private static void ensureAnimationLoop(List<ImageIcon> frames, boolean loop, int delay) {
        if (animationTimer != null && animationTimer.isRunning() && currentFrames == frames) {
            animationTimer.setDelay(delay);
            return;
        }

        if (animationTimer != null) animationTimer.stop();

        currentFrames = frames;
        currentFrameIndex = 0;
        isLooping = loop;

        animationTimer = new Timer(delay, null);
        animationTimer.addActionListener(e -> {
            if (currentFrameIndex >= currentFrames.size()) {
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
            public void nativeMousePressed(NativeMouseEvent e) {
            }

            @Override
            public void nativeMouseReleased(NativeMouseEvent e) {
            }
        });

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
                //Для подсчета нажатий при зажатых клавишах
                handleGlobalClick();
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

    private static void handleGlobalClick() {
        clickCount++;
        prefs.putInt(CLICK_KEY, clickCount);

        SwingUtilities.invokeLater(() -> {
            counterLabel.setText(String.valueOf(clickCount));
            currentFrames = walkSprites.get(selectedColor);

            walkDelay = Math.max(MIN_WALK_DELAY, walkDelay - DELAY_STEP);
            ensureAnimationLoop(currentFrames, true, walkDelay);

            if (delayResetTimer != null && delayResetTimer.isRunning())
                delayResetTimer.stop();

            delayResetTimer = new Timer(300, e -> {
                currentFrames = idleSprites.get(selectedColor);
                idleDelay = walkDelay;
                ensureAnimationLoop(currentFrames, true, idleDelay);

                if (slowDownTimer != null && slowDownTimer.isRunning()) slowDownTimer.stop();

                slowDownTimer = new Timer(500, ev -> {
                    idleDelay = Math.min(idleDelay + DELAY_STEP, DEFAULT_DELAY);
                    walkDelay = idleDelay;

                    animationTimer.setDelay(idleDelay);
                    if (idleDelay == DEFAULT_DELAY) slowDownTimer.stop();
                });
                slowDownTimer.start();
            });

            delayResetTimer.setRepeats(false);
            delayResetTimer.start();
            tryUnlockNewSkin();
        });
    }
}
