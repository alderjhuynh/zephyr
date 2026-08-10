package com.zephyr.client.configplusgui.secretsettings.bettermovement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class Aerodynamics {
    private static final double acceleration = 0.02D;
    private static final boolean AllowElytra = false;

    public static boolean enabled = true;

    private Aerodynamics() {
    }

    public static void onEnable() {
        enabled = true;
    }

    public static void onDisable() {
        enabled = false;
    }

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (!AllowElytra) {
            if (!enabled || player == null || player.onGround() || player.isFallFlying()) {
                return;
            }
        }

        if (AllowElytra) {
            if (!enabled || player == null || player.onGround()) {
                return;
            }
        }

        if (Glide.isGliding) return;

        if (player.isSprinting() || (player.isFallFlying() && AllowElytra && client.options.keySprint.isDown())) {

            Vec3 direction = getBoostDirection(player);
            if (direction.lengthSqr() < 1.0E-6D) {
                return;
            }

            double currentAccel = acceleration;
            if (WaveDash.isBoosted()) {
                currentAccel *= WaveDash.BOOST_MULTIPLIER;
            }

            player.addDeltaMovement(new Vec3(
                    direction.x * currentAccel,
                    direction.y * currentAccel,
                    direction.z * currentAccel
            ));
        }
    }

    private static Vec3 getBoostDirection(LocalPlayer player) {
        Vec3 lookDirection = player.getLookAngle();
        if (lookDirection.lengthSqr() < 1.0E-6D) {
            return Vec3.ZERO;
        }
        return lookDirection.normalize();
    }
}
