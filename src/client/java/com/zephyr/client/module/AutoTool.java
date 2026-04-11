package com.zephyr.client.module;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class AutoTool {
    public static boolean enabled = true;

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void onStartBreakingBlock(BlockState state) {
        if (!enabled || mc.player == null) return;

        PlayerInventory inv = mc.player.getInventory();

        int bestSlot = -1;
        float bestSpeed = 1.0f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);

            if (stack.isEmpty()) continue;

            float speed = stack.getMiningSpeedMultiplier(state);

            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        if (bestSlot != -1 && bestSlot != inv.selectedSlot) {
            inv.selectedSlot = bestSlot;
        }
    }
}