package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class DurabilitySwap {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean enabled = true;

    public static void onTick() {
        if (mc == null || mc.player == null || mc.world == null) return;
        if (!enabled) return;

        ClientPlayerEntity player = mc.player;

        ItemStack stack = player.getMainHandStack();
        if (stack == null || stack.isEmpty()) return;

        if (!stack.isDamageable()) return;

        int max = stack.getMaxDamage();
        int currentDamage = stack.getDamage();
        int remaining = max - currentDamage;

        if (remaining > 1) return;

        int swapSlot = findSafeSlot(player);
        if (swapSlot == -1) return;

        player.getInventory().setSelectedSlot(swapSlot);

        player.sendMessage(Text.literal("Swapped near broken item."), true);
    }

    private static int findSafeSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (i == player.getInventory().getSelectedSlot()) continue;

            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (stack.isDamageable()) {
                int remaining = stack.getMaxDamage() - stack.getDamage();
                if (remaining <= 1) continue;
            }

            return i;
        }
        return -1;
    }
}
