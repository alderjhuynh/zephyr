package com.zephyr.client.module.qol.auramap.waypoint.render;

import com.zephyr.client.module.qol.auramap.AuraMapController;
import com.zephyr.client.module.qol.auramap.waypoint.Waypoint;
import com.zephyr.client.module.qol.auramap.waypoint.WaypointFormat;
import com.zephyr.client.module.qol.auramap.waypoint.WaypointManager;
import com.zephyr.client.module.qol.auramap.waypoint.WaypointIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public final class WaypointMinimapOverlay {
    private WaypointMinimapOverlay() {}

    public static void renderDots(GuiGraphicsExtractor g) {
    }

    public static void renderDot(GuiGraphicsExtractor g, double relX, double relZ, Waypoint w) {
    }

    public static void renderMinimapLabels(GuiGraphicsExtractor g, int x0, int y0, int size,
                                            double px, double pz, float yaw, double zoom, double radiusBlocks,
                                            double ppb) {
        var config = AuraMapController.CONFIG;
        if (config == null || !config.waypointsEnabled) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var font = mc.font;
        boolean showNames = config.waypointLabels;
        double opacity = clamp(config.waypointOpacity / 100.0, 0.1, 1.0);
        int ps = 1;
        int iconPx = 8;
        float ppbF = (float) Math.max(0.125, Math.min(16.0, ppb));
        float cx = x0 + size / 2.0f;
        float cy = y0 + size / 2.0f;
        float rotF = (float) Math.toRadians(180.0 - yaw);
        double cos = Math.cos(rotF), sin = Math.sin(rotF);
        g.enableScissor(x0, y0, x0 + size, y0 + size);
        try {
            int drawn = 0;
            for (Waypoint w : WaypointManager.get().visible()) {
                if (w.disabled()) continue;
                double dx = (w.x() + 0.5) - px;
                double dz = (w.z() + 0.5) - pz;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radiusBlocks * 1.25) continue;
                if (config.waypointMaxDistance > 0 && d > config.waypointMaxDistance
                        && !w.isDestination() && !w.isDeath() && !w.isGlobal()) continue;
                double lx = (dx * ppbF) * cos - (dz * ppbF) * sin;
                double ly = (dx * ppbF) * sin + (dz * ppbF) * cos;
                float sx = (float) (cx + lx);
                float sy = (float) (cy + ly);
                int half = iconPx / 2;
                if (sx < x0 + half || sy < y0 + half || sx > x0 + size - half || sy > y0 + size - half) continue;
                var pose = g.pose();
                pose.pushMatrix();
                try {
                    pose.translate(sx, sy);
                    boolean highlight = w.isDestination() || w.temporary();
                    if (w.isDeath()) {
                        drawSkull(g, font, 0, -half, ps, opacity);
                    } else {
                        WaypointBannerRenderer.drawBanner(g, 0, -half, ps, w.color().argb(), opacity, highlight);
                    }
                    if (showNames) {
                        String name = w.name();
                        if (name.length() > 12) name = name.substring(0, 11) + "…";
                        int tw = font.width(name);
                        int pillW = (tw + 4 + 1) / 2;
                        int pillH = 5;
                        float pillX0 = sx - pillW / 2.0f;
                        float pillY0 = sy + half + 1;
                        if (pillX0 < x0 || pillY0 < y0 || pillX0 + pillW > x0 + size || pillY0 + pillH > y0 + size) {
                            if (++drawn >= 12) break;
                            continue;
                        }
                        pose.pushMatrix();
                        try {
                            pose.translate(0.0f, half + 1.0f);
                            pose.scale(0.5f, 0.5f);
                            g.fill(-tw / 2 - 2, 0, tw / 2 + 2, 10, 0xAA000000);
                            g.text(font, name, -tw / 2, 1, 0xFFFFFFFF, true);
                        } finally {
                            pose.popMatrix();
                        }
                    }
                } finally {
                    pose.popMatrix();
                }
                if (++drawn >= 12) break;
            }
        } finally {
            g.disableScissor();
        }
    }

    public static void renderWorldMap(GuiGraphicsExtractor g, double baseX, double baseZ, double zoom, int width, int height) {
        var mc = Minecraft.getInstance();
        double ppx = mc.player != null ? mc.player.getX() : 0;
        double ppy = mc.player != null ? mc.player.getY() : 0;
        double ppz = mc.player != null ? mc.player.getZ() : 0;
        renderWorldMap(g, baseX, baseZ, zoom, width, height, ppx, ppy, ppz);
    }

    public static void renderWorldMap(GuiGraphicsExtractor g, double baseX, double baseZ, double zoom,
                                      int width, int height, double ppx, double ppy, double ppz) {
        var config = AuraMapController.CONFIG;
        if (config == null || !config.waypointsEnabled) return;
        var mc = Minecraft.getInstance();
        var font = mc.font;
        List<Waypoint> list = WaypointManager.get().visible();
        list.sort((a, b) -> {
            double da = a.distSq(ppx, ppy, ppz);
            double db = b.distSq(ppx, ppy, ppz);
            return Double.compare(db, da);
        });
        int drawn = 0;
        double opacity = clamp(config.waypointOpacity / 100.0, 0.1, 1.0);
        int ps = WaypointBannerRenderer.pixelScale(
                config.waypointIconScale <= 0 ? 1.0 : config.waypointIconScale);
        int iconPx = 8 * ps;
        final int tile = 512;
        for (Waypoint w : list) {
            if (w.disabled()) continue;
            double wx = w.x() + 0.5;
            double wz = w.z() + 0.5;
            int originX = Math.floorDiv(w.x(), tile) * tile;
            int originZ = Math.floorDiv(w.z(), tile) * tile;
            int ix0 = (int) Math.floor(baseX + originX * zoom);
            int iz0 = (int) Math.floor(baseZ + originZ * zoom);
            int ix1 = (int) Math.floor(baseX + (originX + tile) * zoom);
            int iz1 = (int) Math.floor(baseZ + (originZ + tile) * zoom);
            int iw = ix1 - ix0;
            int ih = iz1 - iz0;
            if (iw <= 0 || ih <= 0) continue;
            double fx = (wx - originX) / tile;
            double fz = (wz - originZ) / tile;
            double sxF = ix0 + fx * iw;
            double szF = iz0 + fz * ih;
            if (sxF < -60 || szF < -30 || sxF > width + 60 || szF > height + 40) continue;
            float sx = (float) sxF;
            float sz = (float) szF;
            boolean highlight = w.isDestination() || w.temporary();
            var pose = g.pose();
            pose.pushMatrix();
            try {
                pose.translate(sx, sz);
                int itop = -iconPx / 2;
                if (w.isDeath()) {
                    drawSkull(g, font, 0, itop, ps, opacity);
                } else {
                    WaypointBannerRenderer.drawBanner(g, 0, itop, ps, w.color().argb(), opacity, highlight);
                }

                String label = w.name();
                int tw = font.width(label);
                float lxF = Math.max(2.0f, Math.min(width - tw - 2.0f, sx - tw / 2.0f));
                float localX = lxF - sx;
                int localY = iconPx / 2 + 3;
                pose.pushMatrix();
                try {
                    pose.translate(localX, localY);
                    g.fill(-2, -1, tw + 2, 9, 0xAA000000);
                    g.text(font, label, 0, 0, 0xFFFFFFFF, true);
                    if (mc.player != null && drawn < 40) {
                        double d = Math.sqrt(w.distSq(ppx, ppy, ppz));
                        String ds = WaypointFormat.distance(d, config.waypointDistancePrecision, config.waypointKmThreshold);
                        g.text(font, ds, 0, 10, 0xFFAAAAAA, true);
                    }
                } finally {
                    pose.popMatrix();
                }
            } finally {
                pose.popMatrix();
            }
            drawn++;
            if (drawn >= 60) break;
        }
    }

    private static void drawSkull(GuiGraphicsExtractor g, Font font, int cx, int top, int ps, double opacity) {
        String skull = WaypointIcon.deathGlyph();
        int alpha = (int) Math.round(255 * clamp(opacity, 0.1, 1.0));
        int color = (alpha << 24) | 0xFF5555;
        int tw = font.width(skull);
        var pose = g.pose();
        pose.pushMatrix();
        try {
            pose.translate(cx, top);
            pose.scale((float) ps, (float) ps);
            g.text(font, skull, -tw / 2, 0, color, true);
        } finally {
            pose.popMatrix();
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
