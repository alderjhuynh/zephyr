package com.zephyr.client.module.qol;

import com.llamalad7.mixinextras.sugar.Local;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class DurabilitySwap extends Module {
    public static final DurabilitySwap INSTANCE = new DurabilitySwap();
    private DurabilitySwap() {
        super("Durability Swap", "Saves tools with low durability from being used to mine blocks", Category.QOL);
    }

    @Override
    public void tick(Minecraft client) {
        if (client == null || client.player == null) return;

        ItemStack stack = client.player.getMainHandItem();
        if (stack.isEmpty()) return;
        if (!stack.isDamageableItem()) return;

        int max = stack.getMaxDamage();
        int current = stack.getDamageValue();
        int remaining = max - current;

        if (remaining > 1) return;

        int slot = findSafeSlot(client.player);
        if (slot == -1) return;

        client.player.getInventory().setSelectedSlot(slot);
        client.player.displayClientMessage(Component.literal("Swapped near broken item."), true);
    }

    private static int findSafeSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (i == player.getInventory().getSelectedSlot()) continue;

            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.isDamageableItem()) {
                int remaining = stack.getMaxDamage() - stack.getDamageValue();
                if (remaining <= 1) continue;
            }

            return i;
        }
        return -1;
    }
}
