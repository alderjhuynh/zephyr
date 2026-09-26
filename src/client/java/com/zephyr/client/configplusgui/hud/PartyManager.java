package com.zephyr.client.configplusgui.hud;

import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.ModuleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implements the hidden "party mode" easter eggs toggled from the secret settings screen.
 * Holds four simple flags - rainbow accent cycling, uwu-ified chat text, confetti accent
 * coloring on HUD/toast accents, and a "roulette" that periodically disables a random
 * enabled module - plus the wobble flag. Also tracks the cumulative uwu counter and
 * roulette spin count, which can be surfaced through {@link PlaceholderEngine}.
 */
public final class PartyManager {
    private static final Random RANDOM = new Random();
    private static final long ROULETTE_INTERVAL_NANOS = 5_000_000_000L;
    private static final long RAINBOW_CYCLE_MILLIS = 2000L;

    public static boolean rainbow;
    public static boolean uwu;
    public static boolean confetti;
    public static boolean wobble;
    public static boolean roulette;

    private static int uwuCount;
    private static int rouletteSpins;

    private static long nextRouletteAtNanos = Long.MAX_VALUE;

    private PartyManager() {
    }

    /** Drives the roulette: disables a random module every interval while enabled. */
    public static void tick() {
        if (!roulette) return;
        long now = System.nanoTime();
        if (now < nextRouletteAtNanos) return;

        disableRandomModule();
        nextRouletteAtNanos = now + ROULETTE_INTERVAL_NANOS;
    }

    /** Turns the module-disabling roulette on or off, scheduling its first spin. */
    public static void setRoulette(boolean on) {
        if (roulette == on) return;
        roulette = on;
        nextRouletteAtNanos = on ? System.nanoTime() + ROULETTE_INTERVAL_NANOS : Long.MAX_VALUE;
    }

    /** Picks a random enabled module, disables it and notifies via toast. */
    private static void disableRandomModule() {
        List<Module> enabled = new ArrayList<>();
        for (Module module : ModuleManager.getModules()) {
            if (module.isEnabled()) enabled.add(module);
        }
        if (enabled.isEmpty()) return;

        Module victim = enabled.get(RANDOM.nextInt(enabled.size()));
        victim.setEnabled(false);
        rouletteSpins++;
        NotificationManager.notify(victim.getName(), false);
    }

    /** Returns a time-cycling rainbow accent when the rainbow flag is set, else {@code fallback}. */
    public static int rainbowAccent(int fallback) {
        return rainbow ? hueAccent(fallback) : fallback;
    }

    /** Always returns a rainbow accent (used for confetti mode toast accents). */
    public static int confettiAccent(int fallback) {
        return hueAccent(fallback);
    }

    /** Computes a hue-cycling accent from the current time, keeping the fallback's alpha. */
    private static int hueAccent(int fallback) {
        float hue = (System.currentTimeMillis() % RAINBOW_CYCLE_MILLIS) / (float) RAINBOW_CYCLE_MILLIS;
        return (fallback & 0xFF000000) | hsvToRgb(hue, 0.6f, 0.92f);
    }

    /** Converts a normalized HSV triplet (h in [0,1), s and v in [0,1]) to an RGB int. */
    private static int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;
        float r, g, b;
        switch ((int) Math.floor(h * 6) % 6) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        int red = (int) Math.round((r + m) * 255);
        int green = (int) Math.round((g + m) * 255);
        int blue = (int) Math.round((b + m) * 255);
        return (red << 16) | (green << 8) | blue;
    }

    /**
     * Translates chat text into "uwu" speak (word substitutions, l/r to w, exclamation
     * suffixes, trailing "uwu"). Increments the uwu counter and returns the input unchanged
     * when null or empty.
     */
    public static String uwuify(String input) {
        if (input == null || input.isEmpty()) return input;

        String text = input;
        text = text.replace("Your", "Uwu").replace("your", "uwu")
                .replace("You", "Uwu").replace("you", "uwu");
        text = text.replace("Please", "Pwease").replace("please", "pwease");
        text = text.replace("Like", "Wike").replace("like", "wike");
        text = text.replace("Hello", "Hewwo").replace("hello", "hewwo");
        text = text.replace("Na", "Nya").replace("na", "nya");
        text = text.replace("ove", "uv");
        text = text.replace("R", "W").replace("r", "w");
        text = text.replace("L", "W").replace("l", "w");
        text = text.replace("!", "!!1!1").replace("?", "?? uwu");
        if (!text.endsWith(" uwu")) {
            text = text + " uwu";
        }
        uwuCount++;
        return text;
    }

    /** How many strings have been uwu-ified this session. */
    public static int uwuCount() {
        return uwuCount;
    }

    /** How many modules the roulette has disabled this session. */
    public static int rouletteSpins() {
        return rouletteSpins;
    }
}
