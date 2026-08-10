package com.zephyr.client.configplusgui.secretsettings.bettermovement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * The {@link BetterMovement} wave-dash helper. Landing within a short window after a dash
 * (checked via {@link Dash#getTicksSinceLastDash()}) while looking steeply down starts a
 * boost that lasts {@code BOOST_DURATION_TICKS}. During the boost the player counts as
 * "boosted" ({@link #isBoosted()}), which amplifies {@link Aerodynamics} acceleration by
 * {@link #BOOST_MULTIPLIER}, enables {@link Glide}, and lets
 * {@link DoubleJump} extend the boost mid-air. Entering fluid cancels the boost with a
 * distinct deactivation sound.
 */
public final class WaveDash {

    private static final int WINDOW_TICKS = 5;
    private static final int BOOST_DURATION_TICKS = 60;
    public static final double BOOST_MULTIPLIER = 3.0D;

    private static final float MIN_PITCH_TO_WAVEDASH = 60.0F;

    public static boolean enabled = true;

    private static boolean wasAirborne = false;
    private static int ticksSinceLanding = Integer.MAX_VALUE;
    /** Remaining ticks of the current wave-dash boost; {@code > 0} while boosted. */
    public static int boostTicksRemaining = 0;
    private static boolean canWaveDash = true;

    private WaveDash() {
    }

    /** Whether a wave-dash boost is currently active. */
    public static boolean isBoosted() {
        return boostTicksRemaining > 0;
    }

    /** Remaining boost ticks. */
    public static int getBoostTicksRemaining() {
        return boostTicksRemaining;
    }

    /** Enables the helper and resets wave-dash state. */
    public static void onEnable() {
        enabled = true;
        wasAirborne = false;
        ticksSinceLanding = Integer.MAX_VALUE;
        boostTicksRemaining = 0;
        canWaveDash = true;
    }

    /** Disables the helper and resets wave-dash state. */
    public static void onDisable() {
        enabled = false;
        wasAirborne = false;
        ticksSinceLanding = Integer.MAX_VALUE;
        boostTicksRemaining = 0;
        canWaveDash = true;
    }

    /** Whether the player's camera pitch qualifies as "looking down" for a wave dash. */
    private static boolean isLookingDown(LocalPlayer player) {
        return player.getXRot() >= MIN_PITCH_TO_WAVEDASH;
    }

    /** Times the boost and starts a wave dash on the landing window when looking down. */
    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !enabled) {
            boostTicksRemaining = 0;
            return;
        }

        if (FluidCheck.anyCheck() && boostTicksRemaining > 0) {
            boostTicksRemaining = 0;
            canWaveDash = true;
            SoundEvent wddSound = SoundEvent.createVariableRangeEvent(
                    com.zephyr.Zephyr.id("wavedash_deactivates"));
            client.level.playSound(
                    player,
                    player.blockPosition(),
                    wddSound,
                    SoundSource.MASTER,
                    1.0F,
                    1.0F
            );
            return;
        }

        if (boostTicksRemaining > 0) {
            boostTicksRemaining--;
            if (boostTicksRemaining == 0) {
                canWaveDash = true;
            }
        }

        boolean onGround = player.onGround();

        if (wasAirborne && onGround) {
            if (Dash.getTicksSinceLastDash() <= Dash.getCooldownTicks() / 2) {
                ticksSinceLanding = 0;
            }
        }

        if (onGround && ticksSinceLanding < Integer.MAX_VALUE) {
            if (ticksSinceLanding <= WINDOW_TICKS && canWaveDash && isLookingDown(player)) {
                boostTicksRemaining = BOOST_DURATION_TICKS;
                canWaveDash = false;
                ticksSinceLanding = Integer.MAX_VALUE;

                SoundEvent wdSound = SoundEvent.createVariableRangeEvent(
                        com.zephyr.Zephyr.id("wavedash_activates"));
                client.level.playSound(
                        player,
                        player.blockPosition(),
                        wdSound,
                        SoundSource.MASTER,
                        1.0F,
                        1.0F
                );
            } else {
                ticksSinceLanding++;
                if (ticksSinceLanding > WINDOW_TICKS) {
                    ticksSinceLanding = Integer.MAX_VALUE;
                }
            }
        }

        wasAirborne = !onGround;
    }
}
