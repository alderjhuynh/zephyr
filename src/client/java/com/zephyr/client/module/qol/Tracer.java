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
 */
public final class Tracer extends Module {
    public static final Tracer INSTANCE = new Tracer();

    private final NumberSetting range = new NumberSetting("Range", 64, 8, 128, 1);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 5.0, 0.5);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", true);

    private Tracer() {
        super("Tracers", "Draws lines from your crosshair to nearby players", Category.QOL);
        addSetting(range);
        addSetting(lineWidth);
        addSetting(throughWalls);
    }

    /**
     * Draws a gizmo line from the player's eyes to each remote player within
     * range.
     *
     * @param client the Minecraft client instance
     */
    @Override
    public void tick(Minecraft client) {
        if (client.level == null || client.player == null) return;

        LocalPlayer player = client.player;
        int RANGE = (int) Math.round(range.get());
        float WIDTH = lineWidth.get().floatValue();
        int color = GlobalConfig.accent();

        Vec3 start = player.getEyePosition();
        double rangeSq = (double) RANGE * RANGE;

        try (var ignored = client.collectPerTickGizmos()) {
            for (Entity entity : client.level.entitiesForRendering()) {
                if (!(entity instanceof RemotePlayer target) || !target.isAlive()) continue;
                if (player.distanceToSqr(target) > rangeSq) continue;

                var line = Gizmos.line(start, target.getEyePosition(), color, WIDTH);
                if (throughWalls.get()) {
                    line.setAlwaysOnTop();
                }
            }
        }
    }
}
