package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages skin state, click tracking, unlock progress, and persistence.
 */
public class SkinManager {
    private static final Preferences prefs = Preferences.userRoot().node(ShroomCompanion.class.getName());

    private static final String
            CLICK_KEY = "clickCount",
            COLOR_KEY = "selectedColor",
            LAST_UNLOCK_KEY = "lastUnlockTime",
            UNLOCKED_COLORS_KEY = "UnlockedColors";

    private static final long UNLOCK_COOLDOWN_MS = 1800_000;
    private static final List<String> ALL_COLORS = List.of(
            "red", "blue", "green", "light_blue",
            "orange", "pink", "violet", "yellow");

    private static int clickCount;
    private static String selectedColor;
    private static List<String> unlockedColors = new ArrayList<>();
    private static String lastUnlockedColor;
    /** Loads progress from preferences. */
    public static void loadProgress() {
        clickCount = prefs.getInt(CLICK_KEY, 0);
        selectedColor = prefs.get(COLOR_KEY, "red");

        String saved = prefs.get(UNLOCKED_COLORS_KEY, "red");
        unlockedColors = new ArrayList<>(List.of(saved.split(",")));
    }

    /** Returns the currently selected skin color. */
    public static String getSelectedColor() {
        return selectedColor;
    }

    /** Sets and saves the currently selected skin color. */
    public static void setSelectedColor(String color) {
        selectedColor = color;
        prefs.put(COLOR_KEY, color);
    }

    /** Returns the list of unlocked colors. */
    public static List<String> getUnlockedColors() {
        return unlockedColors;
    }

    /**
     * Returns the current click count.
     */
    public static int getClickCount() {
        return clickCount;
    }

    /**
     * Increments click count and saves it.
     */
    public static void incrementClickCount() {
        clickCount++;
        prefs.putInt(CLICK_KEY, clickCount);
    }

    /**
     * Attempts to unlock a new skin if conditions are met.
     * Returns true if a new skin was unlocked.
     */
    public static boolean tryUnlockNewSkin() {
        long now = System.currentTimeMillis();
        long lastUnlock = prefs.getLong(LAST_UNLOCK_KEY, 0);

        if (clickCount < 1000 || now - lastUnlock < UNLOCK_COOLDOWN_MS) return false;

        List<String> locked = new ArrayList<>(ALL_COLORS);
        locked.removeAll(unlockedColors);
        if (locked.isEmpty()) return false;

        clickCount -= 1000;
        prefs.putInt(CLICK_KEY, clickCount);

        lastUnlockedColor = locked.get((int) (Math.random() * locked.size()));
        unlockedColors.add(lastUnlockedColor);
        saveUnlockedColors();

        prefs.putLong(LAST_UNLOCK_KEY, now);
        return true;
    }

    /** Returns the last unlocked skin color. */
    public static String getLastUnlockedColor() {
        return lastUnlockedColor;
    }

    /** Saves unlocked colors to preferences. */
    private static void saveUnlockedColors() {
        prefs.put(UNLOCKED_COLORS_KEY, String.join(",", unlockedColors));
    }

    /** Returns remaining cooldown time in minutes before next unlock. */
    public static long getRemainingCooldownMinutes() {
        long now = System.currentTimeMillis();
        long lastUnlock = prefs.getLong(LAST_UNLOCK_KEY, 0);
        long remaining = UNLOCK_COOLDOWN_MS - (now - lastUnlock);
        return Math.max(0, remaining / 60000); // в минутах
    }

    /** Clears all progress and resets state to default (debug function). */
    public static void resetProgress() {
        try {
            prefs.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

        clickCount = 0;
        selectedColor = "red";
        unlockedColors = new ArrayList<>(List.of("red"));
        lastUnlockedColor = null;
    }
}
