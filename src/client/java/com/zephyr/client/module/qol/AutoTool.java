package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class AutoTool extends Module {
    public static final AutoTool INSTANCE = new AutoTool();
    private AutoTool() {
        super("AutoTool", "Swaps to the correct tool to mine a block", Category.QOL);
    }

    private static final Minecraft client = Minecraft.getInstance();

    public static void onStartBreakingBlock(BlockState state) {
        if (client.player == null) return;

        Inventory inv = client.player.getInventory();

        int bestSlot = -1;
        float bestSpeed = 1.0f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);

            if (stack.isEmpty()) continue;

            float speed = stack.getDestroySpeed(state);

            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        if (bestSlot != -1 && bestSlot != inv.getSelectedSlot()) {
            inv.setSelectedSlot(bestSlot);
        }
    }
}
