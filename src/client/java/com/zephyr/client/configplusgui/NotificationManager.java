package com.zephyr.client.configplusgui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class NotificationManager {
    private static final long SLIDE_DURATION_NANOS = 180_000_000L; // matches ZephyrScreen's panel slide
    private static final long HOLD_DURATION_NANOS = 1_600_000_000L;
    private static final long LIFETIME_NANOS = SLIDE_DURATION_NANOS * 2 + HOLD_DURATION_NANOS;

    private static final int MARGIN = 10;
    private static final int TOAST_WIDTH = 150;
    private static final int TOAST_HEIGHT = 24;
    private static final int TOAST_GAP = 6;
    private static final int MAX_VISIBLE = 5;

    private static final List<Toast> ACTIVE = new CopyOnWriteArrayList<>();

    private NotificationManager() {
    }

    /** Queues a popup for a module that was just toggled via its hotkey. No-op while popups are disabled. */
    public static void notify(String moduleName, boolean enabledNow) {
        if (!GlobalConfig.hotkeyPopupsEnabled()) return;
        ACTIVE.add(0, new Toast(moduleName, enabledNow, System.nanoTime()));
    }

    /** Renders and expires active toasts. Safe to call every frame even when none are active. */
    public static void render(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight) {
        if (ACTIVE.isEmpty()) return;

        long now = System.nanoTime();
        ACTIVE.removeIf(toast -> now - toast.createdAtNanos() >= LIFETIME_NANOS);

        int y = MARGIN;
        int shown = 0;
        for (Toast toast : ACTIVE) {
            if (shown >= MAX_VISIBLE) break;

            int offset = slideOffsetPx(now - toast.createdAtNanos());
            int x = screenWidth - MARGIN - TOAST_WIDTH + offset;
            renderToast(graphics, font, toast, x, y);

            y += TOAST_HEIGHT + TOAST_GAP;
            shown++;
        }
    }

    private static void renderToast(GuiGraphicsExtractor graphics, Font font, Toast toast, int x, int y) {
        graphics.fill(x, y, x + TOAST_WIDTH, y + TOAST_HEIGHT, ZephyrScreen.PANEL_BG);

        int accent = toast.enabledNow() ? ZephyrScreen.accent() : ZephyrScreen.TEXT_DIM;
        graphics.fill(x, y, x + TOAST_WIDTH, y + 2, accent);

        graphics.text(font, toast.moduleName(), x + 8, y + 9, ZephyrScreen.TEXT_MAIN, false);

        String status = toast.enabledNow() ? "ON" : "OFF";
        int statusColor = toast.enabledNow() ? ZephyrScreen.accent() : ZephyrScreen.TEXT_DIM;
        int statusWidth = font.width(status);
        graphics.text(font, status, x + TOAST_WIDTH - 8 - statusWidth, y + 9, statusColor, false);
    }

    private static int slideOffsetPx(long elapsed) {
        if (elapsed < SLIDE_DURATION_NANOS) {
            double t = elapsed / (double) SLIDE_DURATION_NANOS;
            double eased = 1 - Math.pow(1 - t, 3); // ease-out cubic
            return (int) Math.round((1 - eased) * (TOAST_WIDTH + MARGIN));
        }

        long afterHold = elapsed - SLIDE_DURATION_NANOS - HOLD_DURATION_NANOS;
        if (afterHold <= 0) {
            return 0;
        }

        double t = Math.min(1.0, afterHold / (double) SLIDE_DURATION_NANOS);
        double eased = t * t * t; // ease-in cubic, mirrors the entrance on the way out
        return (int) Math.round(eased * (TOAST_WIDTH + MARGIN));
    }

    private record Toast(String moduleName, boolean enabledNow, long createdAtNanos) {
    }
}