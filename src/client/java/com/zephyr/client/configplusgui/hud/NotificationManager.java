package com.zephyr.client.configplusgui.hud;

import com.zephyr.client.configplusgui.config.GlobalConfig;
import com.zephyr.client.configplusgui.config.StealthManager;
import com.zephyr.client.configplusgui.screen.ZephyrScreen;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Renders transient "toast" notifications, used to report hotkey toggles (e.g. "Sprint —
 * ON") and warnings such as keybind conflicts. Toasts queue newest-first, slide in with an
 * ease-out cubic, hold still, then slide out, with both the animation speed and hold
 * duration coming from {@link GlobalConfig}. They anchor to the corner configured by
 * {@link GlobalConfig#notificationCorner()} and expire automatically. Toasts are not shown
 * while hotkey popups are disabled or Stealth Mode is active.
 */
public final class NotificationManager {
    private static final long BASE_SLIDE_DURATION_NANOS = 180_000_000L; // matches ZephyrScreen's panel slide
    /** Floor so a very fast animation speed never reads as a teleport. */
    private static final long MIN_SLIDE_DURATION_NANOS = 40_000_000L;

    private static final int MARGIN = 10;
    private static final int TOAST_WIDTH = 150;
    private static final int TOAST_HEIGHT = 24;
    private static final int TOAST_GAP = 6;
    private static final int MAX_VISIBLE = 5;

    private static final List<Toast> ACTIVE = new CopyOnWriteArrayList<>();

    private NotificationManager() {
    }

    /** Toast slide length, scaled by the Menu Animation Speed setting. */
    private static long slideDurationNanos() {
        return Math.max(MIN_SLIDE_DURATION_NANOS,
                (long) (BASE_SLIDE_DURATION_NANOS * GlobalConfig.animationSpeed()));
    }

    /** How long a toast holds still before sliding out, from the Notification Lifetime setting. */
    private static long holdDurationNanos() {
        return (long) (GlobalConfig.notificationHoldSeconds() * 1_000_000_000L);
    }

    /** Total lifetime: slide-in + hold + slide-out. */
    private static long lifetimeNanos() {
        return slideDurationNanos() * 2 + holdDurationNanos();
    }

    /** Queues a popup for a module that was just toggled via its hotkey. No-op while popups are disabled or Stealth Mode is active. */
    public static void notify(String moduleName, boolean enabledNow) {
        if (!GlobalConfig.hotkeyPopupsEnabled()) return;
        if (StealthManager.isActive()) return;
        ACTIVE.add(0, new ModuleToast(moduleName, enabledNow, System.nanoTime()));
    }

    /**
     * Queues a generic text toast (no ON/OFF status), e.g. an event notification from a
     * module. Unlike {@link #notify} it is not gated by the Hotkey Popups setting, but it
     * is still hidden while Stealth Mode is active.
     *
     * @param text  the message to display
     * @param color the accent color for the toast's top strip
     */
    public static void notifyText(String text, int color) {
        if (StealthManager.isActive()) return;
        ACTIVE.add(0, new MessageToast(text, color, System.nanoTime()));
    }

    /** Renders and expires active toasts. Safe to call every frame even when none are active. */
    public static void render(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight) {
        if (ACTIVE.isEmpty()) return;

        long now = System.nanoTime();
        ACTIVE.removeIf(toast -> now - toast.createdAtNanos() >= lifetimeNanos());

        Corner corner = GlobalConfig.notificationCorner();
        boolean left = corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT;
        boolean bottom = corner == Corner.BOTTOM_LEFT || corner == Corner.BOTTOM_RIGHT;

        int baseX = left ? MARGIN : screenWidth - MARGIN - TOAST_WIDTH;
        int baseY = bottom ? screenHeight - MARGIN - TOAST_HEIGHT : MARGIN;
        int stackDirection = bottom ? -1 : 1;

        int y = baseY;
        int shown = 0;
        for (Toast toast : ACTIVE) {
            if (shown >= MAX_VISIBLE) break;

            int offset = slideOffsetPx(now - toast.createdAtNanos());
            int x = left ? baseX - offset : baseX + offset;
            renderToast(graphics, font, toast, x, y);

            y += stackDirection * (TOAST_HEIGHT + TOAST_GAP);
            shown++;
        }
    }

    /** Draws one toast: background, accent strip (confetti-aware) and status text. */
    private static void renderToast(GuiGraphicsExtractor graphics, Font font, Toast toast, int x, int y) {
        graphics.fill(x, y, x + TOAST_WIDTH, y + TOAST_HEIGHT, ZephyrScreen.PANEL_BG);

        int accent = toast.accent();
        if (PartyManager.confetti) {
            accent = PartyManager.confettiAccent(accent);
        }
        graphics.fill(x, y, x + TOAST_WIDTH, y + 2, accent);

        graphics.text(font, toast.text(), x + 8, y + 9, ZephyrScreen.TEXT_MAIN, false);

        String status = toast.status();
        if (status.isEmpty()) return;
        int statusWidth = font.width(status);
        graphics.text(font, status, x + TOAST_WIDTH - 8 - statusWidth, y + 9, accent, false);
    }

    /** Horizontal offset for a toast at {@code elapsed} nanos: slides in, pauses, slides out. */
    private static int slideOffsetPx(long elapsed) {
        long slide = slideDurationNanos();
        if (elapsed < slide) {
            double t = elapsed / (double) slide;
            double eased = 1 - Math.pow(1 - t, 3); // ease-out cubic
            return (int) Math.round((1 - eased) * (TOAST_WIDTH + MARGIN));
        }

        long afterHold = elapsed - slide - holdDurationNanos();
        if (afterHold <= 0) {
            return 0;
        }

        double t = Math.min(1.0, afterHold / (double) slide);
        double eased = t * t * t; // ease-in cubic, mirrors the entrance on the way out
        return (int) Math.round(eased * (TOAST_WIDTH + MARGIN));
    }

    /** A single queued notification: the displayed text, its accent color, and its birth time. */
    private interface Toast {
        /** The message text drawn in the toast body. */
        String text();

        /** The accent color for the top strip and the status text. */
        int accent();

        /** A short right-aligned status label (e.g. "ON"), or an empty string to hide it. */
        String status();

        /** The tick time the toast was queued, used for the slide in/out animation. */
        long createdAtNanos();
    }

    /** Toast backing a module hotkey toggle, showing the module name and an ON/OFF status. */
    private record ModuleToast(String moduleName, boolean enabledNow, long createdAtNanos) implements Toast {
        @Override
        public String text() {
            return moduleName;
        }

        @Override
        public int accent() {
            return enabledNow ? ZephyrScreen.accent() : ZephyrScreen.TEXT_DIM;
        }

        @Override
        public String status() {
            return enabledNow ? "ON" : "OFF";
        }
    }

    /** Toast backing a generic event message with a fixed accent color and no status label. */
    private record MessageToast(String message, int color, long createdAtNanos) implements Toast {
        @Override
        public String text() {
            return message;
        }

        @Override
        public int accent() {
            return color;
        }

        @Override
        public String status() {
            return "";
        }
    }
}