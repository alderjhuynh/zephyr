package com.zephyr.client.configplusgui.secretsettings.bettermovement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The {@link BetterMovement} air-acceleration helper. While the player is airborne,
 * sprinting and not gliding or flying, it pushes the player along the look direction each
 * tick, giving a smooth "air control" feel. The per-tick acceleration is multiplied by
 * {@link WaveDash#BOOST_MULTIPLIER} while a wave-dash boost is active. Elytra boosting is
 * disabled by the {@code AllowElytra} constant.
 */
public final class Aerodynamics {
    private static final double acceleration = 0.02D;
    private static final boolean AllowElytra = false;

    public static boolean enabled = true;

    private Aerodynamics() {
    }

    /** Marks the helper enabled. */
    public static void onEnable() {
        enabled = true;
    }

    /** Marks the helper disabled. */
    public static void onDisable() {
        enabled = false;
    }

    /** Applies the per-tick air-acceleration when the airborne sprint conditions hold. */
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

    /** The normalized look direction used as the boost vector, or {@link Vec3#ZERO} when looking straight up/down. */
    private static Vec3 getBoostDirection(LocalPlayer player) {
        Vec3 lookDirection = player.getLookAngle();
        if (lookDirection.lengthSqr() < 1.0E-6D) {
            return Vec3.ZERO;
        }
        return lookDirection.normalize();
    }
}
