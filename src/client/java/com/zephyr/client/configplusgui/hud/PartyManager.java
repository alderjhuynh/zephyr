package com.zephyr.client.configplusgui.hud;

import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.ModuleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Runtime-only "party mode" state exposed by the hidden secret menu. Everything here
 * is a session easter egg: nothing is persisted, and every flag resets on launch.
 * <p>
 * Holds the {@code rainbow} / {@code uwu} / {@code confetti} / {@code wobble} /
 * {@code roulette} toggles, the {@link #rainbowAccent(int)} hue-cycling helper used
 * by {@link com.zephyr.client.configplusgui.config.GlobalConfig} (and the toast
 * border) so rainbow reaches every panel, toast and HUD element without touching
 * persistence, the {@link #uwuify(String)} chat transform, and the module roulette
 * timer that periodically picks a random enabled module and disables it. {@link #tick()}
 * is driven from ZephyrClient's existing client tick.
 */
public final class PartyManager {
    private static final Random RANDOM = new Random();
    /** How long the roulette waits between spins. */
    private static final long ROULETTE_INTERVAL_NANOS = 5_000_000_000L;
    /** Full hue cycle length; 2000ms keeps the rainbow lively without strobe. */
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

    /** Called every client tick; drives the module roulette timer. */
    public static void tick() {
        if (!roulette) return;
        long now = System.nanoTime();
        if (now < nextRouletteAtNanos) return;

        disableRandomModule();
        nextRouletteAtNanos = now + ROULETTE_INTERVAL_NANOS;
    }

    public static void setRoulette(boolean on) {
        if (roulette == on) return;
        roulette = on;
        nextRouletteAtNanos = on ? System.nanoTime() + ROULETTE_INTERVAL_NANOS : Long.MAX_VALUE;
    }

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

    /**
     * Hue-cycling accent override. When rainbow mode is off this just returns the
     * fallback untouched, so every call site behaves exactly as before.
     */
    public static int rainbowAccent(int fallback) {
        return rainbow ? hueAccent(fallback) : fallback;
    }

    /** Always-rainbow accent used by the confetti toast border, regardless of the rainbow flag. */
    public static int confettiAccent(int fallback) {
        return hueAccent(fallback);
    }

    private static int hueAccent(int fallback) {
        float hue = (System.currentTimeMillis() % RAINBOW_CYCLE_MILLIS) / (float) RAINBOW_CYCLE_MILLIS;
        return (fallback & 0xFF000000) | hsvToRgb(hue, 0.6f, 0.92f);
    }

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

    /** Uwu-ifies outgoing chat. Commands are intentionally left alone by the mixin. */
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

    /** How many messages have been uwu-ified this session. */
    public static int uwuCount() {
        return uwuCount;
    }

    /** How many modules the roulette has spun off this session. */
    public static int rouletteSpins() {
        return rouletteSpins;
    }
}
