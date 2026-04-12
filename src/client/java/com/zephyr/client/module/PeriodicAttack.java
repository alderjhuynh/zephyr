package com.zephyr.client.module;

import com.zephyr.client.ZephyrConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

public class PeriodicAttack {
    private static final int MIN_DELAY_TICKS = 1;
    private static final int MAX_DELAY_TICKS = 200;

    public static boolean enabled = false;

    public static int delayTicks = 20;
    private static int tickCounter = 0;


    public static void tick(MinecraftClient client) {
        if (!enabled) {
            tickCounter = 0;
            return;
        }

        if (client == null || client.player == null || client.world == null) return;

        tickCounter++;

        if (tickCounter >= delayTicks) {
            tickCounter = 0;
            simulateAttack(client);
        }
    }

    public static int getDelayTicks() {
        return delayTicks;
    }

    public static void setDelayTicks(int value) {
        delayTicks = Math.max(MIN_DELAY_TICKS, Math.min(MAX_DELAY_TICKS, value));
        ZephyrConfig.saveCurrentState();
    }

    public static int getMinDelayTicks() {
        return MIN_DELAY_TICKS;
    }

    public static int getMaxDelayTicks() {
        return MAX_DELAY_TICKS;
    }

    private static void simulateAttack(MinecraftClient client) {
        if (client.crosshairTarget == null) return;

        if (client.player == null) return;

        switch (client.crosshairTarget.getType()) {
            case ENTITY -> {
                var entityHit = (net.minecraft.util.hit.EntityHitResult) client.crosshairTarget;
                client.interactionManager.attackEntity(client.player, entityHit.getEntity());
                client.player.swingHand(Hand.MAIN_HAND);
            }
            case BLOCK -> {
                var blockHit = (net.minecraft.util.hit.BlockHitResult) client.crosshairTarget;
                client.interactionManager.attackBlock(
                        blockHit.getBlockPos(),
                        blockHit.getSide()
                );
                client.player.swingHand(Hand.MAIN_HAND);
            }
            case MISS -> {
                client.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }
}
