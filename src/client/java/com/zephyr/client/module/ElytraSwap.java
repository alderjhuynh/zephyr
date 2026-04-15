package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ElytraSwap {

    public static boolean enabled = false;

    public static void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        World world = client.world;

        if (player == null || world == null) return;
        if (!enabled) return;

        BlockPos belowPos = player.getBlockPos().down();
        boolean isAirBelow = world.getBlockState(belowPos).isAir();

        ItemStack equippedChest = player.getEquippedStack(EquipmentSlot.CHEST);

        if (isAirBelow) {
            if (equippedChest.getItem() != Items.ELYTRA) {
                int slot = findItem(player, Items.ELYTRA);
                if (slot != -1) {
                    equipArmor(client, slot);
                }
            }
        } else {
            if (equippedChest.getItem() == Items.ELYTRA) {
                int slot = findChestplate(player);
                if (slot != -1) {
                    equipArmor(client, slot);
                }
            }
        }
    }

    private static int findItem(ClientPlayerEntity player, Item target) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == target) {
                return i;
            }
        }

        for (int i = 9; i < 36; i++) {
            if (player.getInventory().getStack(i).getItem() == target) {
                return i;
            }
        }

        return -1;
    }

    private static int findChestplate(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (isChestplate(player.getInventory().getStack(i))) {
                return i;
            }
        }

        for (int i = 9; i < 36; i++) {
            if (isChestplate(player.getInventory().getStack(i))) {
                return i;
            }
        }

        return -1;
    }

    private static boolean isChestplate(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.DIAMOND_CHESTPLATE ||
                item == Items.NETHERITE_CHESTPLATE ||
                item == Items.IRON_CHESTPLATE ||
                item == Items.GOLDEN_CHESTPLATE ||
                item == Items.CHAINMAIL_CHESTPLATE ||
                item == Items.LEATHER_CHESTPLATE;
    }

    private static void equipArmor(MinecraftClient client, int invSlot) {
        if (client.interactionManager == null || client.player == null) return;

        int syncId = client.player.currentScreenHandler.syncId;

        int slotIndex = invSlot < 9 ? invSlot + 36 : invSlot;

        client.interactionManager.clickSlot(syncId, slotIndex, 0, SlotActionType.PICKUP, client.player);

        client.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, client.player);

        client.interactionManager.clickSlot(syncId, slotIndex, 0, SlotActionType.PICKUP, client.player);
    }
}