package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Automatically swaps to the fastest tool in the player's hotbar for the block
 * currently being broken. On every block-break start the hotbar is scanned and
 * the selected slot is switched to the item with the highest destroy speed.
 */
public final class AutoTool extends Module {
    public static final AutoTool INSTANCE = new AutoTool();
    private AutoTool() {
        super("AutoTool", "Swaps to the correct tool to mine a block", Category.QOL);
    }

    private static final Minecraft client = Minecraft.getInstance();

    /**
     * Called when the player starts breaking a block; selects the fastest
     * available hotbar tool for that block.
     *
     * @param state the block state being broken
     */
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
