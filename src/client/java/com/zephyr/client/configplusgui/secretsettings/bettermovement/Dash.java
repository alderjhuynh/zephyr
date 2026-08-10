package com.zephyr.client.configplusgui.secretsettings.bettermovement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * The {@link BetterMovement} mid-air dash helper. Triggered by pressing Sneak while
 * airborne: on the transition from sneaking to airborne-sneaking it launches the player
 * hard along the look direction (reversed while holding Down), plays a dash sound, spawns
 * cloud particles and starts a cooldown. The result feeds {@link WaveDash}, which checks
 * {@link #getTicksSinceLastDash()} shortly after landing to decide whether a wave dash is
 * available.
 */
public final class Dash {
    private static final double STRENGTH = 0.6D;
    private static final boolean ALLOW_ELYTRA = false;
    private static final int COOLDOWN_TICKS = 40;

    public static boolean enabled = true;

    private static int cooldownTimer = 0;
    private static int ticksSinceLastDash = Integer.MAX_VALUE;

    private static boolean wasAirborne = false;
    private static boolean wasSneaking = false;

    private Dash() {
    }

    /** Ticks since the last dash; near {@code Integer.MAX_VALUE} until the first dash. */
    public static int getTicksSinceLastDash() {
        return ticksSinceLastDash;
    }

    /** Remaining cooldown ticks before the next dash is allowed. */
    public static int getCooldownTimer() {
        return cooldownTimer;
    }

    /** The fixed dash cooldown length in ticks. */
    public static int getCooldownTicks() {
        return COOLDOWN_TICKS;
    }

    /** Enables the helper and resets all dash state. */
    public static void onEnable() {
        enabled = true;
        cooldownTimer = 0;
        ticksSinceLastDash = Integer.MAX_VALUE;
        wasAirborne = false;
        wasSneaking = false;
    }

    /** Disables the helper and resets all dash state. */
    public static void onDisable() {
        enabled = false;
        cooldownTimer = 0;
        ticksSinceLastDash = Integer.MAX_VALUE;
        wasAirborne = false;
        wasSneaking = false;
    }

    /** Ticks timers and fires the dash on the sneaking-while-airborne edge press. */
    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;

        if (ticksSinceLastDash < Integer.MAX_VALUE) ticksSinceLastDash++;

        if (cooldownTimer > 0) {
            cooldownTimer--;
        }

        if (player == null || !enabled) {
            wasAirborne = false;
            wasSneaking = false;
            return;
        }

        if (FluidCheck.anyCheck()) {
            wasAirborne = false;
            wasSneaking = false;
            return;
        }

        boolean isAirborne = !player.onGround();
        boolean sneakPressed = client.options.keyShift.isDown();

        if (player.isFallFlying() || Glide.isGliding) {
            wasAirborne = false;
            wasSneaking = false;
            return;
        }

        boolean dashTriggered = isAirborne && sneakPressed
                && wasAirborne && !wasSneaking;

        wasAirborne = isAirborne;
        wasSneaking = sneakPressed;

        if (!dashTriggered) return;
        if (cooldownTimer > 0) return;

        Vec3 direction = getBoostDirection(player);
        if (direction.lengthSqr() < 1.0E-6D) return;

        if (!client.options.keyDown.isDown()) {
            player.addDeltaMovement(new Vec3(
                    direction.x * STRENGTH,
                    direction.y * STRENGTH,
                    direction.z * STRENGTH
            ));
        } else {
            player.addDeltaMovement(new Vec3(
                    direction.x * -STRENGTH,
                    direction.y * -STRENGTH,
                    direction.z * -STRENGTH
            ));
        }

        SoundEvent DASH_SOUND = SoundEvent.createVariableRangeEvent(
                com.zephyr.Zephyr.id("dash"));

        client.level.playSound(
                player,
                player.blockPosition(),
                DASH_SOUND,
                SoundSource.MASTER,
                1.0F,
                1.0F
        );

        var random = player.getRandom();
        for (int i = 0; i < 5; i++) {
            double vx = random.nextGaussian() * 0.05D;
            double vy = random.nextGaussian() * 0.05D;
            double vz = random.nextGaussian() * 0.05D;
            client.level.addParticle(
                    ParticleTypes.CLOUD,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    vx, vy, vz
            );
        }

        cooldownTimer = COOLDOWN_TICKS;
        ticksSinceLastDash = 0;
    }

    /** The normalized look direction used as the dash vector, or {@link Vec3#ZERO} when looking straight up/down. */
    public static Vec3 getBoostDirection(LocalPlayer player) {
        Vec3 lookDirection = player.getLookAngle();
        if (lookDirection.lengthSqr() < 1.0E-6D) {
            return Vec3.ZERO;
        }
        return lookDirection.normalize();
    }
}
