package com.zephyr.client;

import net.minecraft.client.MinecraftClient;

public class Sprint {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    public static boolean enabled = true;

    public void onTick() {
        if (!enabled) return;
        if (mc.player == null) return;
        if (mc.player.isTouchingWater()) return;

        boolean isMoving =
                mc.player.input.movementForward != 0 ||
                mc.player.input.movementSideways != 0;

        mc.player.setSprinting(isMoving);
    }
}