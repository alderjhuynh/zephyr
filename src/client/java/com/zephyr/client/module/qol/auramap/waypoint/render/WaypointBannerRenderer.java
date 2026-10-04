package com.zephyr.client.module.qol.auramap.waypoint.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class WaypointBannerRenderer {
    private WaypointBannerRenderer() {}

    public static int pixelScale(double iconScale) {
        int ps = (int) Math.round(Math.max(0.5, Math.min(4.0, iconScale)));
        return Math.max(1, ps);
    }

    public static void drawBanner(GuiGraphicsExtractor g, int cx, int top, int ps,
                                  int fillArgb, double opacity, boolean highlight) {
        int x0 = cx - 4 * ps;
        int alpha = (int) Math.round(255 * Math.max(0.1, Math.min(1.0, opacity)));
        int fill = (alpha << 24) | (fillArgb & 0xFFFFFF);
        int outline = 0xFF000000;

        if (highlight) {
            g.fill(x0 - 1, top - 1, x0 + 8 * ps + 1, top, 0xFFFFFFFF);
            g.fill(x0 - 1, top + 8 * ps, x0 + 8 * ps + 1, top + 8 * ps + 1, 0xFFFFFFFF);
            g.fill(x0 - 1, top, x0, top + 8 * ps, 0xFFFFFFFF);
            g.fill(x0 + 8 * ps, top, x0 + 8 * ps + 1, top + 8 * ps, 0xFFFFFFFF);
        }

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int kind = bannerPixel(x, y);
                if (kind == 0) continue;
                int c = kind == 1 ? outline : fill;
                int px0 = x0 + x * ps;
                int py0 = top + y * ps;
                g.fill(px0, py0, px0 + ps, py0 + ps, c);
            }
        }
    }

    public static void drawDeathX(GuiGraphicsExtractor g, int cx, int top, int ps, double opacity) {
        int x0 = cx - 4 * ps;
        int alpha = (int) Math.round(255 * Math.max(0.1, Math.min(1.0, opacity)));
        int red = (alpha << 24) | 0xE02020;
        int outline = 0xFF000000;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int d1 = Math.abs(x - y);
                int d2 = Math.abs((7 - x) - y);
                int m = Math.min(d1, d2);
                if (m > 1) continue;
                int c = m == 0 ? red : outline;
                int px0 = x0 + x * ps;
                int py0 = top + y * ps;
                g.fill(px0, py0, px0 + ps, py0 + ps, c);
            }
        }
        g.fill(x0, top, x0 + 8 * ps, top + 1, 0xFFFFFFFF);
        g.fill(x0, top + 8 * ps - 1, x0 + 8 * ps, top + 8 * ps, 0xFFFFFFFF);
    }

    private static int bannerPixel(int x, int y) {
        if (y == 0) return (x >= 1 && x <= 6) ? 1 : 0;
        if (y >= 1 && y <= 5) {
            if (x == 2 || x == 5) return 1;
            if (x == 3 || x == 4) return 2;
            return 0;
        }
        if (x == 3 || x == 4) return 1;
        return 0;
    }
}
