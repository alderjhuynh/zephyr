package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class ItemRestock {
    private static final int HOTBAR_SIZE = 9;
    private static final int OFFHAND_SCREEN_SLOT = 45;
    private static final int HOTBAR_SCREEN_SLOT_OFFSET = 36;
    private static final int MAX_PENDING_USE_TICKS = 40;
    private static final int MAX_TOTEM_CHECK_TICKS = 5;

    public static boolean enabled = false;

    private static final PendingUse MAIN_HAND_USE = new PendingUse(Hand.MAIN_HAND);
    private static final PendingUse OFF_HAND_USE = new PendingUse(Hand.OFF_HAND);

    private static ItemStack previousMainHand = ItemStack.EMPTY;
    private static ItemStack previousOffHand = ItemStack.EMPTY;
    private static int pendingTotemCheckTicks;

    private ItemRestock() {
    }

    public static void trackUse(MinecraftClient mc, Hand hand, ItemStack stack, ActionResult result) {
        if (!enabled || !result.isAccepted() || result == ActionResult.SUCCESS_NO_ITEM_USED || mc.player == null || stack.isEmpty()) {
            return;
        }

        getPendingUse(hand).begin(stack);
    }

    public static void captureUseAttempt(Hand hand, ItemStack stack) {
        if (!enabled || stack.isEmpty()) {
            return;
        }

        getPendingUse(hand).capture(stack);
    }

    public static void onTotemPop(MinecraftClient mc) {
        if (!enabled || mc.player == null) {
            return;
        }

        pendingTotemCheckTicks = MAX_TOTEM_CHECK_TICKS;
    }

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null) {
            clearState();
            return;
        }

        if (pendingTotemCheckTicks > 0 && handleTotemRestock(mc, player)) {
            pendingTotemCheckTicks = 0;
        } else if (pendingTotemCheckTicks > 0) {
            pendingTotemCheckTicks--;
        }

        MAIN_HAND_USE.tick(mc, player);
        OFF_HAND_USE.tick(mc, player);

        previousMainHand = player.getMainHandStack().copy();
        previousOffHand = player.getOffHandStack().copy();
    }

    private static boolean handleTotemRestock(MinecraftClient mc, ClientPlayerEntity player) {
        boolean handled = false;

        if (previousMainHand.isOf(Items.TOTEM_OF_UNDYING) && !player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            restockHand(mc, player, Hand.MAIN_HAND, previousMainHand);
            handled = true;
        }

        if (previousOffHand.isOf(Items.TOTEM_OF_UNDYING) && !player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            restockHand(mc, player, Hand.OFF_HAND, previousOffHand);
            handled = true;
        }

        return handled;
    }

    private static void clearState() {
        MAIN_HAND_USE.clear();
        OFF_HAND_USE.clear();
        previousMainHand = ItemStack.EMPTY;
        previousOffHand = ItemStack.EMPTY;
        pendingTotemCheckTicks = 0;
    }

    private static PendingUse getPendingUse(Hand hand) {
        return hand == Hand.MAIN_HAND ? MAIN_HAND_USE : OFF_HAND_USE;
    }

    private static void restockHand(MinecraftClient mc, PlayerEntity player, Hand hand, ItemStack template) {
        if (!enabled || template.isEmpty()) {
            return;
        }

        ScreenHandler handler = player.currentScreenHandler;
        if (handler == null || !handler.getCursorStack().isEmpty()) {
            return;
        }

        int targetInventorySlot = getHandInventorySlot(player.getInventory(), hand);
        int sourceInventorySlot = findRestockSource(player.getInventory(), template, targetInventorySlot);
        if (sourceInventorySlot < 0) {
            return;
        }

        int sourceScreenSlot = toScreenSlot(sourceInventorySlot);
        int targetScreenSlot = toScreenSlot(targetInventorySlot);
        if (sourceScreenSlot < 0 || targetScreenSlot < 0) {
            return;
        }

        mc.interactionManager.clickSlot(handler.syncId, sourceScreenSlot, 0, SlotActionType.PICKUP, player);
        mc.interactionManager.clickSlot(handler.syncId, targetScreenSlot, 0, SlotActionType.PICKUP, player);
        mc.interactionManager.clickSlot(handler.syncId, sourceScreenSlot, 0, SlotActionType.PICKUP, player);
    }

    private static int findRestockSource(PlayerInventory inventory, ItemStack template, int excludedInventorySlot) {
        int bestSlot = -1;
        int bestCount = -1;
        int oppositeHandSlot = excludedInventorySlot == PlayerInventory.OFF_HAND_SLOT ? inventory.selectedSlot : PlayerInventory.OFF_HAND_SLOT;

        for (int slot = 0; slot < inventory.size(); slot++) {
            if (slot == excludedInventorySlot || slot == oppositeHandSlot) {
                continue;
            }

            ItemStack candidate = inventory.getStack(slot);
            if (candidate.isEmpty() || !matchesForRestock(candidate, template)) {
                continue;
            }

            if (candidate.getCount() > bestCount) {
                bestSlot = slot;
                bestCount = candidate.getCount();
            }
        }

        return bestSlot;
    }

    private static boolean matchesForRestock(ItemStack candidate, ItemStack template) {
        return ItemStack.areItemsAndComponentsEqual(candidate.copyWithCount(1), template.copyWithCount(1));
    }

    private static int getHandInventorySlot(PlayerInventory inventory, Hand hand) {
        return hand == Hand.MAIN_HAND ? inventory.selectedSlot : PlayerInventory.OFF_HAND_SLOT;
    }

    private static int toScreenSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < HOTBAR_SIZE) {
            return HOTBAR_SCREEN_SLOT_OFFSET + inventorySlot;
        }

        if (inventorySlot >= HOTBAR_SIZE && inventorySlot < 36) {
            return inventorySlot;
        }

        if (inventorySlot == PlayerInventory.OFF_HAND_SLOT) {
            return OFFHAND_SCREEN_SLOT;
        }

        return -1;
    }

    private static final class PendingUse {
        private final Hand hand;
        private ItemStack capturedTemplate = ItemStack.EMPTY;
        private ItemStack template = ItemStack.EMPTY;
        private int ticksRemaining;

        private PendingUse(Hand hand) {
            this.hand = hand;
        }

        private void capture(ItemStack stack) {
            this.capturedTemplate = stack.copy();
        }

        private void begin(ItemStack stack) {
            this.template = stack.copy();
            this.ticksRemaining = MAX_PENDING_USE_TICKS;
            this.capturedTemplate = ItemStack.EMPTY;
        }

        private void tick(MinecraftClient mc, ClientPlayerEntity player) {
            if (this.template.isEmpty()) {
                return;
            }

            ItemStack current = player.getStackInHand(this.hand);
            if (didConsumeFromHand(current)) {
                restockHand(mc, player, this.hand, this.template);
                clear();
                return;
            }

            if (isStillUsing(player, current)) {
                return;
            }

            this.ticksRemaining--;
            if (this.ticksRemaining <= 0 || !isSameTrackedItem(current)) {
                clear();
            }
        }

        private boolean didConsumeFromHand(ItemStack current) {
            if (current.isEmpty()) {
                return true;
            }

            return isSameTrackedItem(current) && current.getCount() < this.template.getCount();
        }

        private boolean isStillUsing(ClientPlayerEntity player, ItemStack current) {
            return player.isUsingItem()
                    && player.getActiveHand() == this.hand
                    && isSameTrackedItem(current);
        }

        private boolean isSameTrackedItem(ItemStack stack) {
            return !stack.isEmpty() && matchesForRestock(stack, this.template);
        }

        private void clear() {
            this.capturedTemplate = ItemStack.EMPTY;
            this.template = ItemStack.EMPTY;
            this.ticksRemaining = 0;
        }
    }

    public static ItemStack consumeCapturedUse(Hand hand) {
        PendingUse pendingUse = getPendingUse(hand);
        ItemStack captured = pendingUse.capturedTemplate;
        pendingUse.capturedTemplate = ItemStack.EMPTY;
        return captured;
    }
}
