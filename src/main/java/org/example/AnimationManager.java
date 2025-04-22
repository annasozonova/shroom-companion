package org.example;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles sprite animation for the character, including walk and idle transitions.
 */
public class AnimationManager {
    private static final int DEFAULT_DELAY = 150;
    private static final int MIN_WALK_DELAY = 50;
    private static final int DELAY_STEP = 2;

    private static List<ImageIcon> currentFrames = new ArrayList<>();
    private static int currentFrameIndex = 0;
    private static Timer animationTimer;
    private static Timer delayResetTimer;
    private static Timer slowDownTimer;

    private static int walkDelay = DEFAULT_DELAY;
    private static int idleDelay = DEFAULT_DELAY;

    private static JLabel characterLabel;

    /** Sets the label to apply animation frames to. */
    public static void setCharacterLabel(JLabel label) {
        characterLabel = label;
    }

    /** Returns the label currently used for animation. */
    public static JLabel getCharacterLabel() {
        return characterLabel;
    }

    /** Sets the active animation frames. */
    public static void setCurrentFrames(List<ImageIcon> frames) {
        if (currentFrames == frames) return;
        currentFrames = frames;
    }

    /** Starts the idle animation loop with current frames. */
    public static void playIdle() {
        ensureAnimationLoop(currentFrames, true, idleDelay);
    }

    /**
     * Plays the walk animation with acceleration, then returns to idle.
     * Executes a callback after walk animation transitions.
     */
    public static void playWalk(List<ImageIcon> walkFrames, Runnable afterWalk) {
        walkDelay = Math.max(MIN_WALK_DELAY, walkDelay - DELAY_STEP);
        setCurrentFrames(walkFrames);
        ensureAnimationLoop(walkFrames, true, walkDelay);

        if (delayResetTimer != null && delayResetTimer.isRunning())
            delayResetTimer.stop();

        delayResetTimer = new Timer(300, e -> {
            List<ImageIcon> idleFrames = ClickCharacterApp.getIdleSprites().get(SkinManager.getSelectedColor());
            setCurrentFrames(idleFrames);
            ensureAnimationLoop(idleFrames, true, idleDelay = walkDelay);

            if (slowDownTimer != null && slowDownTimer.isRunning())
                slowDownTimer.stop();

            slowDownTimer = new Timer(500, ev -> {
                idleDelay = Math.min(idleDelay + DELAY_STEP, DEFAULT_DELAY);
                walkDelay = idleDelay;
                if (animationTimer != null) {
                    animationTimer.setDelay(idleDelay);
                    if (idleDelay == DEFAULT_DELAY) slowDownTimer.stop();
                }
            });
            slowDownTimer.start();

            if (afterWalk != null) afterWalk.run();
        });

        delayResetTimer.setRepeats(false);
        delayResetTimer.start();
    }

    /**
     * Ensures the animation loop is running with the specified delay.
     * Replaces existing loop if frames changed or timer stopped.
     */
    private static void ensureAnimationLoop(List<ImageIcon> frames, boolean loop, int delay) {
        if (animationTimer != null && animationTimer.isRunning() && currentFrames == frames) {
            animationTimer.setDelay(delay);
            return;
        }

        if (animationTimer != null) animationTimer.stop();

        currentFrames = frames;
        currentFrameIndex = 0;

        animationTimer = new Timer(delay, e -> {
            if (currentFrameIndex >= currentFrames.size()) {
                if (loop) currentFrameIndex = 0;
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
}
