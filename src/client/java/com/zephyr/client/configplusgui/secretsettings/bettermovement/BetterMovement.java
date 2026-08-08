package com.zephyr.client.configplusgui.secretsettings.bettermovement;

import net.minecraft.client.Minecraft;

public final class BetterMovement {
    public static boolean enabled;

    private BetterMovement() {
    }

    public static void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public static void onEnable() {
        Aerodynamics.onEnable();
        Dash.onEnable();
        DoubleJump.onEnable();
        WaveDash.onEnable();
        Glide.onEnable();
    }

    public static void onDisable() {
        Aerodynamics.onDisable();
        Dash.onDisable();
        DoubleJump.onDisable();
        WaveDash.onDisable();
        Glide.onDisable();
        NoFall.noFallTicks = 0;
    }

    public static void tick(Minecraft client) {
        if (!enabled) return;

        Aerodynamics.tick(client);
        Dash.tick(client);
        DoubleJump.tick(client);
        WaveDash.tick(client);
        Glide.tick(client);

        NoFall.noFallTicks = 2;
    }
}
