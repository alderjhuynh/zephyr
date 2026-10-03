package com.zephyr.client.cornerstone;

import java.util.Optional;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

// 1.21.1 port: sendOverlayMessage -> displayClientMessage(msg, true).
public final class CornerstoneSelection {
    private static boolean active;
    private static BlockPos first;
    private static BlockPos second;

    private CornerstoneSelection() {}

    public static void register() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!active || !level.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (!pos.equals(first)) {
                first = pos.immutable();
                announce(player, "Corner 1", first);
            }
            return InteractionResult.FAIL;
        });

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!active || !level.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (hand == InteractionHand.MAIN_HAND && !hit.getBlockPos().equals(second)) {
                second = hit.getBlockPos().immutable();
                announce(player, "Corner 2", second);
            }
            return InteractionResult.FAIL;
        });
    }

    private static void announce(Player player, String label, BlockPos pos) {
        player.displayClientMessage(Component.literal(label + " set: " + pos.toShortString()), true);
    }

    public static boolean toggle() {
        active = !active;
        return active;
    }

    public static boolean isActive() {
        return active;
    }

    public static void clear() {
        first = null;
        second = null;
    }

    public static Optional<BlockPos> first() {
        return Optional.ofNullable(first);
    }

    public static Optional<BlockPos> second() {
        return Optional.ofNullable(second);
    }

    public static void setFirst(BlockPos pos) {
        first = pos.immutable();
    }

    public static void setSecond(BlockPos pos) {
        second = pos.immutable();
    }
}
