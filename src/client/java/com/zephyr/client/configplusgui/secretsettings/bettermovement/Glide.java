package com.zephyr.client.configplusgui.secretsettings.bettermovement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

/**
 * The {@link BetterMovement} glide helper. Pressing Space while airborne with a
 * {@link WaveDash} boost active starts a glide (requiring the double-jump budget to be
 * largely consumed, see {@link #canStartGlide}), during which the player is pushed forward
 * along their current horizontal velocity each tick. The glide persists through brief
 * Space releases (grace window) and plays a looping {@link GlideSound}. It ends on
 * landing, entering fluid, falling, or when the boost expires.
 */
public final class Glide {
    private static final double ACCELERATION = 0.04D;
    private static final double VERTICAL_BOOST = 0.05D;

    private static final int SPACE_RELEASE_GRACE_TICKS = 5;

    private static SoundEvent GLIDE_SOUND = SoundEvent.createVariableRangeEvent(
            com.zephyr.Zephyr.id("entity_glides"));
    private static GlideSound activeSoundInstance = null;

    public static boolean enabled = true;

    /** Whether a glide is currently in progress. */
    public static boolean isGliding = false;
    /** Whether Space was held on the previous tick. */
    public static boolean spaceWasPressed = false;
    private static int spaceReleasedTicks = 0;

    private Glide() {
    }

    /** Enables the helper. */
    public static void onEnable() {
        enabled = true;
    }

    /** Disables the helper, stopping any active glide. */
    public static void onDisable() {
        enabled = false;
        stopGliding();
        spaceWasPressed = false;
        spaceReleasedTicks = 0;
    }

    /** Tracks the Space input and applies the glide thrust while gliding. */
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

    /** Begins the looping glide sound, tracking the active instance. */
    private static void startSound(Minecraft client, LocalPlayer player) {
        activeSoundInstance = new GlideSound(player, GLIDE_SOUND);
        client.getSoundManager().play(activeSoundInstance);
    }

    /** Ends the glide; the looping sound instance stops itself on its next tick. */
    private static void stopGliding() {
        isGliding = false;
        activeSoundInstance = null;
    }

    /** Whether a glide may begin: enabled, airborne, boosted, not flying, and the double-jump budget mostly spent. */
    public static boolean canStartGlide(LocalPlayer player) {
        return enabled
                && !player.onGround()
                && !player.isFallFlying()
                && WaveDash.isBoosted()
                && DoubleJump.getTicksAvailable() <= 39;
    }

    /** Whether an ongoing glide may persist: still enabled, airborne, not flying and still boosted. */
    public static boolean canContinueGlide(LocalPlayer player) {
        return enabled
                && !player.onGround()
                && !player.isFallFlying()
                && WaveDash.isBoosted();
    }

    /** The player's current horizontal velocity direction (Y zeroed), or {@link Vec3#ZERO} when idle. */
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
