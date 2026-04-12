package com.zephyr.client.module;

import com.zephyr.client.ZephyrConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

public class FastAttack {
    private static final int MIN_TIMES_PER_TICK = 1;
    private static final int MAX_TIMES_PER_TICK = 20;

    public static boolean enabled = false;
    public static int TimesPerTick = 10;


    public static void tick(MinecraftClient client) {
        if (!enabled || client == null || client.player == null || client.world == null || client.interactionManager == null) {
            return;
        }

        for (int i = 0; i < TimesPerTick; i++) {
            simulateAttack(client);
        }
    }

    public static int getTimesPerTick() {
        return TimesPerTick;
    }

    public static void setTimesPerTick(int value) {
        TimesPerTick = Math.max(MIN_TIMES_PER_TICK, Math.min(MAX_TIMES_PER_TICK, value));
        ZephyrConfig.saveCurrentState();
    }

    public static int getMinTimesPerTick() {
        return MIN_TIMES_PER_TICK;
    }

    public static int getMaxTimesPerTick() {
        return MAX_TIMES_PER_TICK;
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
