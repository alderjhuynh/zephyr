package com.zephyr.client.module;

import com.zephyr.client.ZephyrConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class FastUse {
    private static final int MIN_TIMES_PER_TICK = 1;
    private static final int MAX_TIMES_PER_TICK = 20;

    public static boolean enabled = false;
    public static int TimesPerTick = 10;

    public static void tick(MinecraftClient client) {
        if (!enabled || client == null || client.player == null || client.world == null || client.interactionManager == null) {return;}
        for (int i = 0; i < TimesPerTick; i++) {
            simulateUse(client);
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
