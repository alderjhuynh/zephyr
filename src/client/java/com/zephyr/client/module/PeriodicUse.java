package com.zephyr.client.module;

import com.zephyr.client.ZephyrConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;

public class PeriodicUse {
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
            simulateUse(client);
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

    private static void simulateUse(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager im = client.interactionManager;

        if (player == null || im == null) return;

        HitResult hit = client.crosshairTarget;

        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            im.interactBlock(player, Hand.MAIN_HAND, (BlockHitResult) hit);
        } else {
            im.interactItem(player, Hand.MAIN_HAND);
        }

        player.swingHand(Hand.MAIN_HAND);
    }
}
