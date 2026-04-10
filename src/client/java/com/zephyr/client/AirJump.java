package com.zephyr.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class AirJump {
    public static boolean enabled = false;

    private static int level = 0;

    private static boolean wasJumpPressed = false;

    public static void setEnabled(boolean value) {
        enabled = value;
        if (enabled) {
            onEnable(MinecraftClient.getInstance());
            return;
        }

        wasJumpPressed = false;
    }

    public static void tick(MinecraftClient client) {
        if (!enabled) return;

        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (player.isOnGround() || !NoFall.enabled) {
            wasJumpPressed = false;
            return;
        }

        boolean isJumpPressed = client.options.jumpKey.isPressed();

        if (isJumpPressed && !wasJumpPressed) {
            int currentY = player.getBlockPos().getY();

            if (currentY != level) {
                level = currentY;
            }

            player.jump();
        }

        wasJumpPressed = isJumpPressed;

        if (client.options.sneakKey.isPressed()) {
            level--;
        }
    }

    public static void onEnable(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player != null) {
            level = player.getBlockPos().getY();
        }

        wasJumpPressed = false;
    }
}
