package com.zephyr.client.module;

import com.zephyr.client.ZephyrConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class LongJump {
    public static boolean enabled = false;

    private static final double MIN_MOMENTUM = 0.1D;
    private static final double MAX_MOMENTUM = 2.0D;
    private static final double DEFAULT_MOMENTUM = 0.6D;

    private static double momentum = DEFAULT_MOMENTUM;
    private static boolean wasJumpPressed = false;

    private LongJump() {
    }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (!enabled || player == null) {
            wasJumpPressed = false;
            return;
        }

        boolean isJumpPressed = client.options.jumpKey.isPressed();

        if (isJumpPressed && !wasJumpPressed && player.isOnGround()) {
            applyForwardMomentum(player);
        }

        wasJumpPressed = isJumpPressed;
    }

    public static void onEnable() {
        wasJumpPressed = false;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (enabled) {
            onEnable();
            return;
        }

        wasJumpPressed = false;
    }

    public static double getMomentum() {
        return momentum;
    }

    public static void setMomentum(double value) {
        momentum = clamp(value, MIN_MOMENTUM, MAX_MOMENTUM);
        ZephyrConfig.saveCurrentState();
    }

    public static double getMinMomentum() {
        return MIN_MOMENTUM;
    }

    public static double getMaxMomentum() {
        return MAX_MOMENTUM;
    }

    private static void applyForwardMomentum(ClientPlayerEntity player) {
        Vec3d direction = getBoostDirection(player);
        if (direction.lengthSquared() < 1.0E-6D) {
            return;
        }

        Vec3d currentVelocity = player.getVelocity();
        player.setSprinting(true);
        player.setVelocity(direction.x * momentum, currentVelocity.y, direction.z * momentum);
        player.velocityModified = true;
    }

    private static Vec3d getBoostDirection(ClientPlayerEntity player) {
        Vec3d lookDirection = player.getRotationVector();
        if (lookDirection.lengthSquared() < 1.0E-6D) {
            float yawRadians = player.getYaw() * 0.017453292F;
            return new Vec3d(-MathHelper.sin(yawRadians), 0.0D, MathHelper.cos(yawRadians));
        }
        return lookDirection.normalize();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
