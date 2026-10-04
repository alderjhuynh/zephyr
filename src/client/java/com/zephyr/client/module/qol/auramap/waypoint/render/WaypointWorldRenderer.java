package com.zephyr.client.module.qol.auramap.waypoint.render;

import com.zephyr.client.module.qol.auramap.AuraMapController;
import com.zephyr.client.module.qol.auramap.waypoint.Waypoint;
import com.zephyr.client.module.qol.auramap.waypoint.WaypointFormat;
import com.zephyr.client.module.qol.auramap.waypoint.WaypointIcon;
import com.zephyr.client.module.qol.auramap.waypoint.WaypointManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class WaypointWorldRenderer {
    private WaypointWorldRenderer() {}

    private static final int FULLBRIGHT = 0xF000F0;
    private static final int MAX_DRAWN = 32;
    private static long lastDebugMs;

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(WaypointWorldRenderer::collect);
    }

    private static void collect(LevelRenderContext ctx) {
        if (!AuraMapController.isActive()) return;
        var config = AuraMapController.CONFIG;
        if (config == null || !config.waypointsEnabled || !config.waypointWorldLabels) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        PoseStack pose = ctx.poseStack();
        var collector = ctx.submitNodeCollector();
        if (pose == null || collector == null) return;
        var camState = ctx.levelState().cameraRenderState;
        if (camState == null || !camState.initialized || camState.pos == null
                || camState.orientation == null) return;

        double camX = camState.pos.x;
        double camY = camState.pos.y;
        double camZ = camState.pos.z;
        double px = mc.player.getX();
        double pz = mc.player.getZ();

        Vector3f fwd = new Vector3f(0, 0, -1);
        camState.orientation.transform(fwd);
        double fx = fwd.x, fy = fwd.y, fz = fwd.z;
        double lookThreshold = Math.cos(Math.toRadians(
                Math.max(1.0, Math.min(45.0, config.waypointLookAngleDeg))));

        Font font = mc.font;
        double opacity = Math.max(0.1, Math.min(1.0, config.waypointOpacity / 100.0));

        List<Candidate> out = new ArrayList<>();
        for (Waypoint w : WaypointManager.get().visible()) {
            if (w.disabled()) continue;
            double wx = w.x() + 0.5;
            double wz = w.z() + 0.5;
            double dx2 = wx - px;
            double dz2 = wz - pz;
            double dist2d = Math.sqrt(dx2 * dx2 + dz2 * dz2);
            if (config.waypointMinWorldDistance > 0 && dist2d < config.waypointMinWorldDistance) continue;
            if (config.waypointMaxDistance > 0 && dist2d > config.waypointMaxDistance) {
                if (!w.isDestination() && !w.isDeath() && !w.isGlobal()) continue;
            }
            double wy = w.yIncluded() ? w.y() + 1.0 : camY;
            double dx = wx - camX;
            double dy = wy - camY;
            double dz = wz - camZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < 4.0) continue;
            if (camState.cullFrustum != null) {
                AABB box = new AABB(wx - 0.5, wy - 0.5, wz - 0.5, wx + 0.5, wy + 0.5, wz + 0.5);
                if (!camState.cullFrustum.isVisible(box)) continue;
            }
            double dist = Math.sqrt(distSq);
            double lookCos = dist > 1e-6 ? (dx * fx + dy * fy + dz * fz) / dist : 1.0;
            boolean highlighted = lookCos >= lookThreshold;
            double playerY = mc.player.getY();
            double dyPlayer = w.yIncluded() ? (w.y() - playerY) : 0.0;
            double dist3d = Math.sqrt(dx2 * dx2
                    + (w.yIncluded() ? dyPlayer * dyPlayer : 0.0) + dz2 * dz2);
            boolean nearby = dist3d <= 20.0 && !config.waypointShortDistances;
            out.add(new Candidate(w, wx, wy, wz, distSq, dist, dist2d,
                    highlighted, nearby, dyPlayer));
        }

        out.sort((a, b) -> Double.compare(a.distSq, b.distSq));
        if (out.size() > MAX_DRAWN) out = out.subList(0, MAX_DRAWN);

        long now = System.currentTimeMillis();
        if (now - lastDebugMs > 30000) {
            lastDebugMs = now;
            int visible = WaypointManager.get().visible().size();
            if (visible > 0) {
                com.zephyr.client.module.qol.auramap.AuraMapBridge.LOGGER.info(
                        "[auramap] waypoints submit: visible={} drawn={} cam=({},{},{})",
                        visible, out.size(),
                        String.format("%.1f", camX), String.format("%.1f", camY),
                        String.format("%.1f", camZ));
            }
        }

        for (Candidate c : out) {
            try {
                drawOne(pose, collector, font, config, camState, c, camX, camY, camZ, opacity);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void drawOne(PoseStack pose,
                                net.minecraft.client.renderer.SubmitNodeCollector collector,
                                Font font,
                                com.zephyr.client.module.qol.auramap.config.AuraMapConfig config,
                                net.minecraft.client.renderer.state.level.CameraRenderState camState,
                                Candidate c, double camX, double camY, double camZ,
                                double opacity) {
        Waypoint w = c.w;

        String glyph = w.isDeath() ? WaypointIcon.deathGlyph() : w.initials();
        if (glyph == null) glyph = "";
        if (glyph.isBlank() && !w.isDeath()) {
            String n = w.name();
            glyph = n.substring(0, Math.min(1, n.length())).toUpperCase();
        }
        if (glyph.length() > 2) glyph = glyph.substring(0, 2);

        int argb = w.color().argb();
        int alpha = (int) Math.round(255 * opacity);
        int markerBg = (alpha << 24) | (argb & 0xFFFFFF);
        if (c.highlighted) markerBg = brighten(markerBg);

        float iconScale = (float) Math.max(0.5, Math.min(4.0, config.waypointIconScale));
        float nameScale = (float) Math.max(0.5, Math.min(4.0, config.waypointNameScale));
        float distScale = (float) Math.max(0.5, Math.min(4.0, config.waypointDistanceScale));
        float t = (float) Math.max(1.0, Math.min(30.0, c.dist * 0.12));
        float s = 0.025f * t * iconScale;

        pose.pushPose();
        try {
            pose.translate(c.wx - camX, c.wy - camY, c.wz - camZ);
            pose.rotate(camState.orientation);
            pose.scale(s, -s, s);

            float gw = font.width(glyph);
            collector.submitText(pose, -gw / 2.0f, -4.0f,
                    Component.literal(glyph).getVisualOrderText(),
                    false, Font.DisplayMode.SEE_THROUGH, FULLBRIGHT,
                    0xFFFFFFFF, markerBg, 0);

            String name = null;
            String distStr = null;
            if (c.nearby) {
                name = w.name();
            } else if (c.highlighted) {
                distStr = WaypointFormat.distance(c.dist,
                        config.waypointDistancePrecision, config.waypointKmThreshold);
                if (config.waypointKeepNames || w.isDeath()) name = w.name();
                else if (config.waypointLabels) name = w.name();
            } else {
                if (config.waypointLabels && c.dist2d >= 3) name = w.name();
            }

            float y = 7.0f;
            String arrow = dyArrow(c.dyPlayer, w.yIncluded());
            if (name != null && !arrow.isEmpty()) name = name + arrow;
            else if (name == null && distStr != null && !arrow.isEmpty()) distStr = distStr + arrow;
            else if (name == null && distStr == null && !arrow.isEmpty()) name = arrow.trim();
            if (name != null) {
                pose.pushPose();
                try {
                    pose.translate(0.0f, y, 0.0f);
                    pose.scale(nameScale, nameScale, 1.0f);
                    float lw = font.width(name);
                    collector.submitText(pose, -lw / 2.0f, 0.0f,
                            Component.literal(name).getVisualOrderText(),
                            false, Font.DisplayMode.SEE_THROUGH, FULLBRIGHT,
                            0xFFFFFFFF, 0x5C000000, 0);
                } finally {
                    pose.popPose();
                }
                y += 10.0f * nameScale;
            }
            if (distStr != null) {
                pose.pushPose();
                try {
                    pose.translate(0.0f, y, 0.0f);
                    pose.scale(distScale, distScale, 1.0f);
                    float lw = font.width(distStr);
                    collector.submitText(pose, -lw / 2.0f, 0.0f,
                            Component.literal(distStr).getVisualOrderText(),
                            false, Font.DisplayMode.SEE_THROUGH, FULLBRIGHT,
                            0xFFFFFFFF, 0x5C000000, 0);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            pose.popPose();
        }
    }

    private static String dyArrow(double dyPlayer, boolean yIncluded) {
        if (!yIncluded) return "";
        if (dyPlayer > 4) return " \u25B2";
        if (dyPlayer < -4) return " \u25BC";
        return "";
    }

    private static int brighten(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 40);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 40);
        int b = Math.min(255, (argb & 0xFF) + 40);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static final class Candidate {
        final Waypoint w;
        final double wx, wy, wz, distSq, dist, dist2d, dyPlayer;
        final boolean highlighted;
        final boolean nearby;
        Candidate(Waypoint w, double wx, double wy, double wz,
                  double distSq, double dist, double dist2d,
                  boolean highlighted, boolean nearby, double dyPlayer) {
            this.w = w;
            this.wx = wx;
            this.wy = wy;
            this.wz = wz;
            this.distSq = distSq;
            this.dist = dist;
            this.dist2d = dist2d;
            this.highlighted = highlighted;
            this.nearby = nearby;
            this.dyPlayer = dyPlayer;
        }
    }
}
