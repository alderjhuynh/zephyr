package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec2f;

public class Sprint {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    public static boolean enabled = true;

    public void onTick() {
        if (!enabled) return;
        if (mc.player == null) return;
        if (mc.player.isTouchingWater()) return;

        Vec2f movementInput = mc.player.input.getMovementInput();
        boolean isMoving = movementInput.x != 0.0F || movementInput.y != 0.0F;

        mc.player.setSprinting(isMoving);
    }
}
