package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

public class ElytraMace {

    private final MinecraftClient client = MinecraftClient.getInstance();

    private static final double ATTACK_RANGE = 4.5;
    private static final double CONE_ANGLE_DEG = 60.0;

    public PlayerEntity findTargetInCone() {
        ClientPlayerEntity self = client.player;
        if (self == null || client.world == null) return null;

        Box searchBox = self.getBoundingBox().expand(ATTACK_RANGE);

        List<PlayerEntity> players = client.world.getEntitiesByClass(
                PlayerEntity.class,
                searchBox,
                p -> p != self && p.isAlive()
        );

        return players.stream()
                .filter(p -> isInViewCone(self, p, CONE_ANGLE_DEG))
                .min(Comparator.comparingDouble(self::distanceTo))
                .orElse(null);
    }

    public boolean isInViewCone(ClientPlayerEntity self, PlayerEntity target, double angleDeg) {
        Vec3d lookVec = self.getRotationVec(1.0f).normalize();
        Vec3d toTarget = target.getEntityPos().subtract(self.getEntityPos()).normalize();

        double dot = lookVec.dotProduct(toTarget);
        double angle = Math.toDegrees(Math.acos(dot));

        return angle <= (angleDeg / 2.0);
    }

    public int findHotbarItem(Item targetItem) {
        ClientPlayerEntity player = client.player;
        if (player == null) return -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == targetItem) {
                return i;
            }
        }

        return -1;
    }

    public int findAnyChestplate() {
        ClientPlayerEntity player = client.player;
        if (player == null) return -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);

            if (stack.getItem() == Items.DIAMOND_CHESTPLATE ||
                    stack.getItem() == Items.NETHERITE_CHESTPLATE ||
                    stack.getItem() == Items.IRON_CHESTPLATE ||
                    stack.getItem() == Items.GOLDEN_CHESTPLATE ||
                    stack.getItem() == Items.CHAINMAIL_CHESTPLATE ||
                    stack.getItem() == Items.LEATHER_CHESTPLATE) {
                return i;
            }
        }

        return -1;
    }

    public int findAxe() {
        ClientPlayerEntity player = client.player;
        if (player == null) return -1;

        for (int i = 0; i < 9; i++) {
            Item item = player.getInventory().getStack(i).getItem();
            if (item == Items.NETHERITE_AXE ||
                    item == Items.DIAMOND_AXE ||
                    item == Items.IRON_AXE) {
                return i;
            }
        }

        return -1;
    }

    public int findMace() {
        return findHotbarItem(Items.MACE);
    }


    public void equipChestplateFromHotbar() {
        int slot = findAnyChestplate();
        if (slot == -1) return;

        swapHotbarIntoChest(slot);
    }

    public void equipBestElytra() {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        int bestSlot = -1;
        int bestScore = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() != Items.ELYTRA) continue;

            int score = 0;

            // I decided I really didn't wanna do this

            /*
            if (EnchantmentHelper.getLevel(Enchantments.MENDING, stack) > 0) score += 2;
            if (EnchantmentHelper.getLevel(Enchantments.UNBREAKING, stack) > 0) score += 1;
            */

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            swapHotbarIntoChest(bestSlot);
        }
    }

    private void swapHotbarIntoChest(int hotbarSlot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.interactionManager == null || mc.player == null) return;

        int screenSlot = 36 + hotbarSlot;

        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                screenSlot,
                0,
                net.minecraft.screen.slot.SlotActionType.PICKUP,
                mc.player
        );

        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                6,
                0,
                net.minecraft.screen.slot.SlotActionType.PICKUP,
                mc.player
        );

        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                screenSlot,
                0,
                net.minecraft.screen.slot.SlotActionType.PICKUP,
                mc.player
        );
    }
}