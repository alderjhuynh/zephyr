package com.zephyr.client.configplusgui.secretsettings.bettermovement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The {@link BetterMovement} double-jump helper. Grants a mid-air re-jump after leaving
 * the ground, gated by a shared "tick budget" ({@code MAX_TICKS_AVAILABLE} total, each
 * jump costs {@code JUMP_TICK_COST}). A second jump can be triggered either by releasing
 * and re-pressing Space while airborne, or by falling at least {@code fallBlocksThreshold}
 * blocks. Re-jumping fires a custom sound and cloud particles; while gliding it also gives
 * a forward boost and extends the {@link WaveDash} boost.
 */
public final class DoubleJump {
    public static boolean enabled = true;
    private static boolean wasJumpPressed = false;
    private static boolean hasLeftGround = false;
    private static boolean jumpReleasedAfterAirborne = false;
    private static double GLIDING_BOOST_STRENGTH = 0.07d;
    private static int EXTRA_AERODYNAMICS_TIME = 40;

    private static final int MAX_TICKS_AVAILABLE = 80;
    private static final int JUMP_TICK_COST = 40;
    private static int ticksAvailable = MAX_TICKS_AVAILABLE;

    public static double fallBlocksThreshold = 2.0;
    private static double airborneStartY = Double.NaN;
    private static boolean fallThresholdMet = false;

    private DoubleJump() {
    }

    /** Whether the block at {@code pos} offers no collision, i.e. counts as open air. */
    private static boolean isAir(BlockPos pos, LocalPlayer player) {
        var state = player.level().getBlockState(pos);
        if (state.isAir()) return true;
        var shape = state.getCollisionShape(player.level(), pos);
        return shape.isEmpty();
    }

    /** Probes several points just below the player's bounding box for solid ground. */
    private static boolean isNearGround(LocalPlayer player) {
        AABB bb = player.getBoundingBox();
        double probeY = bb.minY - 0.05;

        double[] xs = { bb.minX + 0.05, bb.maxX - 0.05, (bb.minX + bb.maxX) / 2.0 };
        double[] zs = { bb.minZ + 0.05, bb.maxZ - 0.05, (bb.minZ + bb.maxZ) / 2.0 };

        for (double x : xs) {
            for (double z : zs) {
                BlockPos pos = BlockPos.containing(x, probeY, z);
                if (!isAir(pos, player)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Ticks of the jump budget that have been consumed. */
    public static int getCooldownTimer() {
        return MAX_TICKS_AVAILABLE - ticksAvailable;
    }

    /** The total jump budget in ticks. */
    public static int getCooldownTicks() {
        return MAX_TICKS_AVAILABLE;
    }

    /** Remaining jump-budget ticks (used by {@link Glide#canStartGlide}). */
    public static int getTicksAvailable() {
        return ticksAvailable;
    }

    /** Enables the helper and resets double-jump state. */
    public static void onEnable() {
        enabled = true;
        wasJumpPressed = false;
        ticksAvailable = MAX_TICKS_AVAILABLE;
        airborneStartY = Double.NaN;
        fallThresholdMet = false;
    }

    /** Disables the helper and resets double-jump state. */
    public static void onDisable() {
        enabled = false;
        wasJumpPressed = false;
        hasLeftGround = false;
        jumpReleasedAfterAirborne = false;
        airborneStartY = Double.NaN;
        fallThresholdMet = false;
    }

    /** Tracks airtime/fall state and fires the re-jump on the qualifying Space press edge. */
    public static void tick(Minecraft client) {
        if (!enabled) return;

        LocalPlayer player = client.player;
        if (player == null) return;

        boolean isJumpPressed = client.options.keyJump.isDown();
        boolean onGround = isNearGround(player);

        if (ticksAvailable < MAX_TICKS_AVAILABLE) {
            ticksAvailable++;
        }

        if (FluidCheck.anyCheck()) return;

        if (onGround || player.onClimbable() || player.isInWater() || player.getAbilities().flying) {
            wasJumpPressed = isJumpPressed;
            hasLeftGround = false;
            jumpReleasedAfterAirborne = false;
            airborneStartY = Double.NaN;
            fallThresholdMet = false;
            return;
        }

        if (!onGround && !hasLeftGround) {
            hasLeftGround = true;
            airborneStartY = player.getY();
        }

        if (hasLeftGround && !Double.isNaN(airborneStartY)) {
            double fallen = airborneStartY - player.getY();
            if (fallen >= fallBlocksThreshold) {
                fallThresholdMet = true;
            }
        }

        if (hasLeftGround && wasJumpPressed && !isJumpPressed) {
            jumpReleasedAfterAirborne = true;
        }

        boolean canDoubleJumpByRelease = hasLeftGround && jumpReleasedAfterAirborne;
        boolean canDoubleJumpByFall = hasLeftGround && fallThresholdMet;

        if ((canDoubleJumpByRelease || canDoubleJumpByFall)
                && isJumpPressed
                && !wasJumpPressed
                && ticksAvailable >= JUMP_TICK_COST) {

            ticksAvailable -= JUMP_TICK_COST;

            SoundEvent JUMP_SOUND = SoundEvent.createVariableRangeEvent(
                    com.zephyr.Zephyr.id("double_jump"));

            client.level.playSound(
                    player,
                    player.blockPosition(),
                    JUMP_SOUND,
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

            Vec3 direction = Dash.getBoostDirection(player);
            if (direction.lengthSqr() < 1.0E-6D) return;

            if (player.isFallFlying() || Glide.isGliding) {
                player.addDeltaMovement(new Vec3(
                        direction.x * GLIDING_BOOST_STRENGTH,
                        direction.y * GLIDING_BOOST_STRENGTH,
                        direction.z * GLIDING_BOOST_STRENGTH
                ));
                player.jumpFromGround();
                WaveDash.boostTicksRemaining += EXTRA_AERODYNAMICS_TIME;
            } else {
                player.jumpFromGround();
            }
            jumpReleasedAfterAirborne = false;
            fallThresholdMet = false;
            airborneStartY = player.getY();
        }

        wasJumpPressed = isJumpPressed;
    }
}
