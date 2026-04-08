package com.zephyr.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class Flight {
    //flight is lowkey cooked cus nofall doesn't work with it :skull:
    public static boolean enabled = false;

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final double SPEED = 0.1;

    //ignore this I lowkey didn't wanna fix how this is handled after removing velocity

    private enum Mode {
        ABILITIES,
    }

    private static Mode mode = Mode.ABILITIES;

    public static void onEnable() {
        enabled = true;

        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (mode == Mode.ABILITIES && !player.isSpectator()) {
            player.getAbilities().flying = true;
            player.getAbilities().allowFlying = true;
        }
    }

    public static void onDisable() {
        enabled = false;

        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (mode == Mode.ABILITIES && !player.isSpectator()) {
            player.getAbilities().flying = false;

            if (!player.getAbilities().creativeMode) {
                player.getAbilities().allowFlying = false;
            }

            player.getAbilities().setFlySpeed(0.05f);
        }
    }

    public static void tick() {
        if (!enabled) return;

        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (player.isSpectator()) return;

        player.getAbilities().flying = true;
        player.getAbilities().allowFlying = true;
        player.getAbilities().setFlySpeed((float) SPEED);
    }
}