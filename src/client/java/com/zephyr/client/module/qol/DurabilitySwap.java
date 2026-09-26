package com.zephyr.client.module.qol;

import com.llamalad7.mixinextras.sugar.Local;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Protects nearly-broken tools from breaking while mining. Each tick, if the
 * item in the main hand has one hit of durability left, the player is swapped
 * to the first other hotbar slot holding an item that is not about to break
 * and an overlay message is shown.
 */
public final class DurabilitySwap extends Module {
    public static final DurabilitySwap INSTANCE = new DurabilitySwap();
    private DurabilitySwap() {
        super("Durability Swap", "Saves tools with low durability from being used to mine blocks", Category.QOL);
    }

    /**
     * Checks the main hand item and swaps to a safe hotbar slot when it is about
     * to break.
     *
     * @param client the Minecraft client instance
     */
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
        client.player.sendOverlayMessage(Component.literal("Swapped near broken item."));
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
