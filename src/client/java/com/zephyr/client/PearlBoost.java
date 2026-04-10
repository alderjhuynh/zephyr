package com.zephyr.client;

import com.zephyr.client.mixin.ClientPlayerEntityInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class PearlBoost {
    private static final double MIN_BOOST_VELOCITY = 0.5D;
    private static final double MAX_BOOST_VELOCITY = 20.0D;
    private static final double DEFAULT_BOOST_VELOCITY = 10.35D;

    public static boolean enabled = false;

    private static Phase phase = Phase.IDLE;
    private static Hand pendingHand;
    private static Vec3d previousVelocity = Vec3d.ZERO;
    private static boolean previousSprinting;
    private static boolean replayingUse;
    private static double boostVelocity = DEFAULT_BOOST_VELOCITY;

    private PearlBoost() {
    }

    public static ActionResult queueThrow(MinecraftClient mc, Hand hand) {
        if (!enabled || replayingUse || phase != Phase.IDLE) {
            return ActionResult.PASS;
        }

        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Items.ENDER_PEARL)) {
            return ActionResult.PASS;
        }

        pendingHand = hand;
        previousVelocity = player.getVelocity();
        previousSprinting = player.isSprinting();
        phase = Phase.BOOSTING;
        return ActionResult.SUCCESS;
    }

    public static void tick(MinecraftClient mc) {
        if (phase == Phase.IDLE) {
            return;
        }

        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null || pendingHand == null) {
            reset();
            return;
        }

        if (!player.getStackInHand(pendingHand).isOf(Items.ENDER_PEARL)) {
            reset();
            return;
        }

        if (phase == Phase.BOOSTING) {
            applyTemporaryBoost(player);
            ((ClientPlayerEntityInvoker) player).zephyr$sendMovementPackets();
            phase = Phase.THROWING;
            return;
        }

        replayingUse = true;
        try {
            mc.interactionManager.interactItem(player, pendingHand);
        } finally {
            replayingUse = false;
            player.setVelocity(previousVelocity);
            player.setSprinting(previousSprinting);
            ((ClientPlayerEntityInvoker) player).zephyr$sendMovementPackets();
            reset();
        }
    }

    public static boolean isReplayingUse() {
        return replayingUse;
    }

    public static double getBoostVelocity() {
        return boostVelocity;
    }

    public static void setBoostVelocity(double velocity) {
        boostVelocity = MathHelper.clamp(velocity, MIN_BOOST_VELOCITY, MAX_BOOST_VELOCITY);
        ZephyrConfig.saveCurrentState();
    }

    public static double getMinBoostVelocity() {
        return MIN_BOOST_VELOCITY;
    }

    public static double getMaxBoostVelocity() {
        return MAX_BOOST_VELOCITY;
    }

    private static void applyTemporaryBoost(ClientPlayerEntity player) {
        Vec3d direction = getBoostDirection(player);

        player.setSprinting(true);
        player.setVelocity(direction.multiply(boostVelocity));
    }

    private static Vec3d getBoostDirection(ClientPlayerEntity player) {
        Vec3d lookDirection = player.getRotationVector();
        if (lookDirection.lengthSquared() < 1.0E-6D) {
            return new Vec3d(0.0D, 0.0D, 1.0D);
        }
        return lookDirection.normalize();
    }

    private static void reset() {
        phase = Phase.IDLE;
        pendingHand = null;
        previousVelocity = Vec3d.ZERO;
        previousSprinting = false;
    }

    private enum Phase {
        IDLE,
        BOOSTING,
        THROWING
    }
}
