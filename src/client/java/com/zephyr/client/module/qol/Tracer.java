package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.config.GlobalConfig;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// this is highkey broken in first person, works okay in f5 though

/**
 * Draws colored lines from the player's eye position to every nearby remote
 * player, making them easy to track. The range, line width, and whether the
 * lines are drawn through walls are all configurable; the accent color comes
 * from the global config.
 *
 * <p>Rendering is per-frame via {@code LevelExtractor#extract} (see
 * {@code TracerLevelExtractorMixin}) so it runs at the render framerate
 * (~60-144Hz) instead of the tick rate (20Hz). The collector is already open
 * as {@code LevelExtractor#collectPerFrameMainThreadGizmos()}, so no
 * {@code collectPerTickGizmos()} wrapper is used.
 */
public final class Tracer extends Module {
    public static final Tracer INSTANCE = new Tracer();

    private final NumberSetting range = new NumberSetting("Range", 64, 8, 128, 1);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 5.0, 0.5);
    private final NumberSetting distance = new NumberSetting("Tracer Start Distance", 2, 0, 10, 1);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", true);

    private Tracer() {
        super("Tracers", "Draws lines from your crosshair to nearby players", Category.QOL);
        addSetting(range);
        addSetting(lineWidth);
        addSetting(throughWalls);
        addSetting(distance);
    }

    /**
     * Legacy tick path - intentionally no-ops. Rendering now happens per-frame
     * from the {@code LevelExtractor} mixin to avoid 20Hz stutter.
     *
     * @param client the Minecraft client instance
     */
    @Override
    public void tick(Minecraft client) {
        // no-op: see renderPerFrame(float) called from LevelExtractor mixin
    }

    /**
     * Per-frame entry point called from {@code TracerLevelExtractorMixin} while
     * {@code LevelExtractor.collectPerFrameMainThreadGizmos()} is open.
     *
     * @param client      the Minecraft client instance
     * @param partialTick the frame interpolation factor (0-1) from
     *                    {@code DeltaTracker#getGameTimeDeltaPartialTick(false)}
     */
    public void renderPerFrame(Minecraft client, float partialTick) {
        if (!isEnabled()) return;
        if (client.level == null || client.player == null) return;

        LocalPlayer player = client.player;
        int RANGE = (int) Math.round(range.get());
        float WIDTH = lineWidth.get().floatValue();
        int color = GlobalConfig.accent();

        Vec3 start = getBetterTracerStartLmaoMyOtherOneWasAss(client, partialTick);
        double rangeSq = (double) RANGE * RANGE;

        // No collectPerTickGizmos() wrapper - collector is already the per-frame
        // LevelExtractor.mainThreadGizmos via Minecraft.renderFrame().
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof RemotePlayer target) || !target.isAlive()) continue;
            if (player.distanceToSqr(target) > rangeSq) continue;

            Vec3 end = target.getEyePosition(partialTick);
            var line = Gizmos.line(start, end, color, WIDTH);
            if (throughWalls.get()) {
                line.setAlwaysOnTop();
            }
        }
    }

    private Vec3 getBetterTracerStartLmaoMyOtherOneWasAss(Minecraft client, float partialTick) {
        LocalPlayer player = client.player;
        double doubleDistance = Tracer.INSTANCE.distance.get();
        int intDistance = (int) doubleDistance;

        // Interpolated eye + view vector for smooth per-frame positioning.
        return player.getEyePosition(partialTick).add(
            player.getViewVector(partialTick).scale(intDistance)
        );
    }
}
