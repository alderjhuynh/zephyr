package com.zephyr.client.configplusgui.secretsettings.bettermovement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

public final class Glide {
    private static final double ACCELERATION = 0.04D;
    private static final double VERTICAL_BOOST = 0.05D;

    private static final int SPACE_RELEASE_GRACE_TICKS = 5;

    private static SoundEvent GLIDE_SOUND = SoundEvent.createVariableRangeEvent(
            com.zephyr.Zephyr.id("entity_glides"));
    private static GlideSound activeSoundInstance = null;

    public static boolean enabled = true;

    public static boolean isGliding = false;
    public static boolean spaceWasPressed = false;
    private static int spaceReleasedTicks = 0;

    private Glide() {
    }

    public static void onEnable() {
        enabled = true;
    }

    public static void onDisable() {
        enabled = false;
        stopGliding();
        spaceWasPressed = false;
        spaceReleasedTicks = 0;
    }

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;

        boolean spacePressed = client.options.keyJump.isDown();

        if (player.onGround() || player.isFallFlying() || FluidCheck.anyCheck() || !WaveDash.isBoosted()) {
            stopGliding();
            spaceReleasedTicks = 0;
            spaceWasPressed = spacePressed;
            return;
        }

        if (spacePressed) {
            spaceReleasedTicks = 0;
        } else {
            spaceReleasedTicks++;
            if (spaceReleasedTicks > SPACE_RELEASE_GRACE_TICKS) {
                stopGliding();
                spaceWasPressed = false;
                return;
            }
        }

        if (!isGliding) {
            if (spaceWasPressed) {
                spaceWasPressed = spacePressed;
                return;
            }
            if (spacePressed && canStartGlide(player)) {
                isGliding = true;
                startSound(client, player);
            }
        }

        spaceWasPressed = spacePressed;

        if (isGliding && canContinueGlide(player)) {
            Vec3 direction = getBoostDirection(player);
            if (direction.lengthSqr() < 1.0E-6D) return;

            player.addDeltaMovement(new Vec3(
                    direction.x * ACCELERATION,
                    VERTICAL_BOOST,
                    direction.z * ACCELERATION
            ));
        }
    }

    private static void startSound(Minecraft client, LocalPlayer player) {
        activeSoundInstance = new GlideSound(player, GLIDE_SOUND);
        client.getSoundManager().play(activeSoundInstance);
    }

    private static void stopGliding() {
        isGliding = false;
        activeSoundInstance = null;
    }

    public static boolean canStartGlide(LocalPlayer player) {
        return enabled
                && !player.onGround()
                && !player.isFallFlying()
                && WaveDash.isBoosted()
                && DoubleJump.getTicksAvailable() <= 39;
    }

    public static boolean canContinueGlide(LocalPlayer player) {
        return enabled
                && !player.onGround()
                && !player.isFallFlying()
                && WaveDash.isBoosted();
    }

    public static Vec3 getBoostDirection(LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        double x = velocity.x;
        double z = velocity.z;

        double horizLengthSq = x * x + z * z;
        if (horizLengthSq < 1.0E-6D) {
            return Vec3.ZERO;
        }

        double scale = 1.0 / Math.sqrt(horizLengthSq);
        return new Vec3(x * scale, 0, z * scale);
    }
}
