package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class AirJump {
    public static boolean enabled = false;

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

        if (player.isOnGround() || player.isClimbing() || player.isTouchingWater() || player.getAbilities().flying) {
            wasJumpPressed = false;
            return;
        }

        boolean isJumpPressed = client.options.jumpKey.isPressed();

        if (isJumpPressed && !wasJumpPressed) {
            player.jump();
        }

        wasJumpPressed = isJumpPressed;
    }

    public static void onEnable(MinecraftClient client) {
        wasJumpPressed = false;
    }
}
