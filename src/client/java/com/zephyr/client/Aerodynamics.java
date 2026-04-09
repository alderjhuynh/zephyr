package com.zephyr.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class Aerodynamics {
    private static final double MIN_ACCELERATION = 0.005D;
    private static final double MAX_ACCELERATION = 0.15D;
    private static final double DEFAULT_ACCELERATION = 0.04D;
    private static final boolean AllowElytra = true;

    public static boolean enabled = false;

    private static double acceleration = DEFAULT_ACCELERATION;

    private Aerodynamics() {
    }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (!AllowElytra) {
            if (!enabled || player == null || player.isOnGround() || player.isFallFlying()) {
                return;
            }
        }

        if (AllowElytra) {
            if (!enabled || player == null || player.isOnGround()) {return;}
        }

        if (player.isSprinting() || (player.isFallFlying() && AllowElytra && client.options.sprintKey.isPressed())) {

            Vec3d direction = getBoostDirection(player);
            if (direction.lengthSquared() < 1.0E-6D) {
                return;
            }

            player.addVelocity(
                    direction.x * acceleration,
                    direction.y * acceleration,
                    direction.z * acceleration
            );
            player.velocityModified = true;
        }
    }

    public static double getAcceleration() {
        return acceleration;
    }

    public static void setAcceleration(double value) {
        acceleration = MathHelper.clamp(value, MIN_ACCELERATION, MAX_ACCELERATION);
    }

    public static double getMinAcceleration() {
        return MIN_ACCELERATION;
    }

    public static double getMaxAcceleration() {
        return MAX_ACCELERATION;
    }

    private static Vec3d getBoostDirection(ClientPlayerEntity player) {
        Vec3d lookDirection = player.getRotationVector();
        if (lookDirection.lengthSquared() < 1.0E-6D) {
            return Vec3d.ZERO;
        }
        return lookDirection.normalize();
    }
}
