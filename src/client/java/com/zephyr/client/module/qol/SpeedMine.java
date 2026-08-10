package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.mixin.qol.SpeedMine.ClientLevelAccessor;
import com.zephyr.client.mixin.qol.SpeedMine.CurrentBreakingPosAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Speeds up block breaking using one of two strategies, selected by mode:
 * <ul>
 *   <li>{@link Mode#HASTE} applies a synthetic, invisible Haste effect so the
 *       vanilla breaking speed is multiplied.</li>
 *   <li>{@link Mode#DAMAGE} sends a STOP_DESTROY_BLOCK packet as soon as the
 *       predicted damage passes a threshold, resetting the breaking progress so
 *       the block breaks almost instantly.</li>
 * </ul>
 * The mode is switched cleanly at runtime, cleaning up any synthetic effect or
 * predicted-break state from the previous mode.
 */
public final class SpeedMine extends Module {
    public static final SpeedMine INSTANCE = new SpeedMine();

    private static final float DAMAGE_THRESHOLD = 0.7f;
    private static final int SYNTHETIC_HASTE_AMPLIFIER = 1;
    private static final int SYNTHETIC_HASTE_DURATION = -1;

    private static final Minecraft mc = Minecraft.getInstance();
    private static BlockPos trackedBreakingPos;
    private static Direction trackedBreakingDirection = Direction.UP;
    private static boolean damageTriggered;
    private static boolean syntheticHasteApplied;

    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.HASTE);
    private Mode lastTickedMode = mode.get();

    private SpeedMine() {
        super("Speed Mine", "Speeds up block breaking via synthetic Haste or predicted damage packets", Category.QOL);
        addSetting(mode);
    }

    /** The speeding strategy to use: synthetic Haste or predicted-damage packets. */
    public enum Mode {
        HASTE,
        DAMAGE
    }

    /**
     * Applies the active speeding strategy: maintains the synthetic Haste effect
     * or monitors breaking progress to trigger the early STOP packet.
     *
     * @param client the Minecraft client instance
     */
    @Override
    public void tick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) return;

        Mode currentMode = mode.get();
        if (currentMode != lastTickedMode) {
            if (lastTickedMode == Mode.HASTE) {
                removeSyntheticHaste();
            } else {
                resetDamageState();
            }
            lastTickedMode = currentMode;
        }

        switch (currentMode) {

            case HASTE -> {
                MobEffectInstance haste = client.player.getEffect(MobEffects.HASTE);

                if (haste == null || haste.getAmplifier() < SYNTHETIC_HASTE_AMPLIFIER) {
                    client.player.addEffect(
                            new MobEffectInstance(
                                    MobEffects.HASTE,
                                    SYNTHETIC_HASTE_DURATION,
                                    SYNTHETIC_HASTE_AMPLIFIER,
                                    false,
                                    false,
                                    false
                            )
                    );
                    syntheticHasteApplied = true;
                } else if (!isSyntheticHaste(haste)) {
                    syntheticHasteApplied = false;
                }
            }

            case DAMAGE -> {
                var accessor = (CurrentBreakingPosAccessor) client.gameMode;

                BlockPos pos = accessor.getCurrentBreakingPos();
                float progress = accessor.getCurrentBreakingProgress();

                if (pos == null || progress <= 0.0f) {
                    resetDamageState();
                    return;
                }

                if (!pos.equals(trackedBreakingPos)) {
                    trackedBreakingPos = pos.immutable();
                    trackedBreakingDirection = getFallbackDirection(pos);
                    damageTriggered = false;
                }

                BlockState state = client.level.getBlockState(pos);
                if (state.isAir()) {
                    resetDamageState();
                    return;
                }

                float delta = state.getDestroyProgress(client.player, client.level, pos);

                if (!damageTriggered && progress + delta >= DAMAGE_THRESHOLD) {
                    sendDamageStopPacket(pos, trackedBreakingDirection);
                    damageTriggered = true;
                }
            }
        }
    }

    /** Removes the synthetic Haste effect and clears any damage-packet state on disable. */
    @Override
    protected void onDisable() {
        removeSyntheticHaste();
        resetDamageState();
    }

    /**
     * Tracks outgoing {@link ServerboundPlayerActionPacket} packets to keep the
     * predicted-break state in sync with the server's block breaking.
     *
     * @param packet the packet about to be sent
     */
    public static void onSendPacket(Object packet) {
        if (!INSTANCE.isEnabled() || INSTANCE.mode.get() != Mode.DAMAGE) return;
        if (!(packet instanceof ServerboundPlayerActionPacket p)) return;
        if (mc.getConnection() == null) return;

        if (p.getAction() == Action.START_DESTROY_BLOCK) {
            trackedBreakingPos = p.getPos().immutable();
            trackedBreakingDirection = p.getDirection();
            damageTriggered = false;
            return;
        }

        if (p.getAction() == Action.ABORT_DESTROY_BLOCK && p.getPos().equals(trackedBreakingPos)) {
            resetDamageState();
            return;
        }

        if (p.getAction() == Action.STOP_DESTROY_BLOCK) {
            trackedBreakingPos = p.getPos().immutable();
            trackedBreakingDirection = p.getDirection();
            damageTriggered = true;
        }
    }

    private static void sendDamageStopPacket(BlockPos pos, Direction direction) {
        if (mc.level == null || mc.getConnection() == null) return;

        var levelAccessor = (ClientLevelAccessor) mc.level;
        try (BlockStatePredictionHandler prediction =
                     levelAccessor.zephyr$getBlockStatePredictionHandler().startPredicting()) {
            mc.getConnection().send(
                    new ServerboundPlayerActionPacket(
                            Action.STOP_DESTROY_BLOCK,
                            pos,
                            direction,
                            prediction.currentSequence()
                    )
            );
        }
    }

    private static Direction getFallbackDirection(BlockPos pos) {
        if (mc.hitResult instanceof BlockHitResult blockHitResult
                && blockHitResult.getType() == HitResult.Type.BLOCK
                && blockHitResult.getBlockPos().equals(pos)) {
            return blockHitResult.getDirection();
        }

        return trackedBreakingDirection != null ? trackedBreakingDirection : Direction.UP;
    }

    private static void resetDamageState() {
        trackedBreakingPos = null;
        trackedBreakingDirection = Direction.UP;
        damageTriggered = false;
    }

    private static boolean isSyntheticHaste(MobEffectInstance haste) {
        return haste.getAmplifier() == SYNTHETIC_HASTE_AMPLIFIER
                && haste.getDuration() == SYNTHETIC_HASTE_DURATION
                && !haste.isAmbient()
                && !haste.isVisible()
                && !haste.showIcon();
    }

    private static void removeSyntheticHaste() {
        if (!syntheticHasteApplied || mc.player == null) {
            syntheticHasteApplied = false;
            return;
        }

        MobEffectInstance haste = mc.player.getEffect(MobEffects.HASTE);
        if (haste != null && isSyntheticHaste(haste)) {
            mc.player.removeEffect(MobEffects.HASTE);
        }

        syntheticHasteApplied = false;
    }
}