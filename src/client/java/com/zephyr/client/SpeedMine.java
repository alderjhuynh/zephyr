package com.zephyr.client;

import com.zephyr.client.mixin.CurrentBreakingPosAccessor;
import com.zephyr.client.mixin.ClientWorldAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class SpeedMine {
    private static final float DAMAGE_THRESHOLD = 0.7f;

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static BlockPos trackedBreakingPos;
    private static Direction trackedBreakingDirection = Direction.UP;
    private static boolean damageTriggered;

    public static Mode mode = Mode.OFF;

    public enum Mode {
        OFF,
        HASTE,
        DAMAGE;

        public Mode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }

    public static void tick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        switch (mode) {

            case HASTE -> {
                StatusEffectInstance haste = mc.player.getStatusEffect(StatusEffects.HASTE);

                if (haste == null || haste.getAmplifier() < 1) {
                    mc.player.addStatusEffect(
                            new StatusEffectInstance(
                                    StatusEffects.HASTE,
                                    -1,
                                    1,
                                    false,
                                    false,
                                    false
                            )
                    );
                }
            }

            case DAMAGE -> {
                var im = mc.interactionManager;
                var accessor = (CurrentBreakingPosAccessor) im;

                BlockPos pos = accessor.getCurrentBreakingPos();
                float progress = accessor.getCurrentBreakingProgress();

                if (pos == null || progress <= 0.0f) {
                    resetDamageState();
                    return;
                }

                if (!pos.equals(trackedBreakingPos)) {
                    trackedBreakingPos = pos.toImmutable();
                    trackedBreakingDirection = getFallbackDirection(pos);
                    damageTriggered = false;
                }

                BlockState state = mc.world.getBlockState(pos);
                if (state.isAir()) {
                    resetDamageState();
                    return;
                }

                float delta = state.calcBlockBreakingDelta(mc.player, mc.world, pos);

                if (!damageTriggered && progress + delta >= DAMAGE_THRESHOLD) {
                    sendDamageStopPacket(pos, trackedBreakingDirection);
                    damageTriggered = true;
                }
            }

            case OFF -> {
                resetDamageState();
            }
        }
    }

    public static void onSendPacket(Object packet) {
        if (mode != Mode.DAMAGE) return;
        if (!(packet instanceof PlayerActionC2SPacket p)) return;
        if (mc.getNetworkHandler() == null) return;

        if (p.getAction() == Action.START_DESTROY_BLOCK) {
            trackedBreakingPos = p.getPos().toImmutable();
            trackedBreakingDirection = p.getDirection();
            damageTriggered = false;
            return;
        }

        if (p.getAction() == Action.ABORT_DESTROY_BLOCK && p.getPos().equals(trackedBreakingPos)) {
            resetDamageState();
            return;
        }

        if (p.getAction() == Action.STOP_DESTROY_BLOCK) {
            trackedBreakingPos = p.getPos().toImmutable();
            trackedBreakingDirection = p.getDirection();
            damageTriggered = true;
        }
    }

    public static void cycleMode() {
        setMode(mode.next());
    }

    public static void setMode(Mode value) {
        mode = value == null ? Mode.OFF : value;
        if (mode != Mode.DAMAGE) {
            resetDamageState();
        }
    }

    private static void sendDamageStopPacket(BlockPos pos, Direction direction) {
        if (mc.world == null || mc.getNetworkHandler() == null) return;

        try (PendingUpdateManager pendingUpdateManager = ((ClientWorldAccessor) mc.world).zephyr$getPendingUpdateManager().incrementSequence()) {
            mc.getNetworkHandler().sendPacket(
                    new PlayerActionC2SPacket(
                            Action.STOP_DESTROY_BLOCK,
                            pos,
                            direction,
                            pendingUpdateManager.getSequence()
                    )
            );
        }
    }

    private static Direction getFallbackDirection(BlockPos pos) {
        if (mc.crosshairTarget instanceof BlockHitResult blockHitResult
                && blockHitResult.getType() == HitResult.Type.BLOCK
                && blockHitResult.getBlockPos().equals(pos)) {
            return blockHitResult.getSide();
        }

        return trackedBreakingDirection != null ? trackedBreakingDirection : Direction.UP;
    }

    private static void resetDamageState() {
        trackedBreakingPos = null;
        trackedBreakingDirection = Direction.UP;
        damageTriggered = false;
    }
}
