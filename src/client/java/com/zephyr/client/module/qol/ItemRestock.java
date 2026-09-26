package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Automatically refills the main hand, offhand, or a consumed totem with a
 * matching item from the player's inventory. Use actions are tracked as they
 * are performed; when an item is consumed from a hand, the module performs a
 * container pickup swap to bring in the largest matching stack. Totem pops are
 * detected and restocked within a short window after they occur.
 */
public final class ItemRestock extends Module {
    public static final ItemRestock INSTANCE = new ItemRestock();

    private static final int HOTBAR_SIZE = 9;
    private static final int OFFHAND_SCREEN_SLOT = 45;
    private static final int HOTBAR_SCREEN_SLOT_OFFSET = 36;
    private static final int MAX_PENDING_USE_TICKS = 40;
    private static final int MAX_TOTEM_CHECK_TICKS = 5;

    private static final PendingUse MAIN_HAND_USE = new PendingUse(InteractionHand.MAIN_HAND);
    private static final PendingUse OFF_HAND_USE = new PendingUse(InteractionHand.OFF_HAND);

    private static ItemStack previousMainHand = ItemStack.EMPTY;
    private static ItemStack previousOffHand = ItemStack.EMPTY;
    private static int pendingTotemCheckTicks;

    private ItemRestock() {
        super("Item Restock", "Swaps a totem or item for a matching one from your inventory", Category.QOL);
    }

    /**
     * Records that a use action with the given hand consumed an action, so the
     * held item can be restocked later if it is depleted.
     *
     * @param hand   the hand the item was used from
     * @param stack  the stack that was used
     * @param result the interaction result of the use
     */
    public static void trackUse(Minecraft client, InteractionHand hand, ItemStack stack, InteractionResult result) {
        if (!INSTANCE.isEnabled() || !result.consumesAction() || client.player == null || stack.isEmpty()) {
            return;
        }

        getPendingUse(hand).begin(stack);
    }

    /** Captures the stack used in an interaction attempt so it can be recalled via {@link #consumeCapturedUse}. */
    public static void captureUseAttempt(InteractionHand hand, ItemStack stack) {
        if (!INSTANCE.isEnabled() || stack.isEmpty()) {
            return;
        }

        getPendingUse(hand).capture(stack);
    }

    /** Starts the short totem-restock window after the player's totem pops. */
    public static void onTotemPop(Minecraft client) {
        if (!INSTANCE.isEnabled() || client.player == null) {
            return;
        }

        pendingTotemCheckTicks = MAX_TOTEM_CHECK_TICKS;
    }

    /**
     * Drives the restock logic: handles pending totem restocks, advances any
     * in-progress use tracking, and snapshots the current hand items.
     *
     * @param client the Minecraft client instance
     */
    @Override
    public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            clearState();
            return;
        }

        if (pendingTotemCheckTicks > 0 && handleTotemRestock(client, player)) {
            pendingTotemCheckTicks = 0;
        } else if (pendingTotemCheckTicks > 0) {
            pendingTotemCheckTicks--;
        }

        MAIN_HAND_USE.tick(client, player);
        OFF_HAND_USE.tick(client, player);

        previousMainHand = player.getMainHandItem().copy();
        previousOffHand = player.getOffhandItem().copy();
    }

    /** Clears all tracking state when the module is disabled. */
    @Override
    protected void onDisable() {
        clearState();
    }

    private static boolean handleTotemRestock(Minecraft client, LocalPlayer player) {
        boolean handled = false;

        if (previousMainHand.is(Items.TOTEM_OF_UNDYING) && !player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) {
            restockHand(client, player, InteractionHand.MAIN_HAND, previousMainHand);
            handled = true;
        }

        if (previousOffHand.is(Items.TOTEM_OF_UNDYING) && !player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            restockHand(client, player, InteractionHand.OFF_HAND, previousOffHand);
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

    private static PendingUse getPendingUse(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? MAIN_HAND_USE : OFF_HAND_USE;
    }

    private static void restockHand(Minecraft client, Player player, InteractionHand hand, ItemStack template) {
        if (!INSTANCE.isEnabled() || template.isEmpty()) {
            return;
        }

        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || !menu.getCarried().isEmpty()) {
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

        client.gameMode.handleContainerInput(menu.containerId, sourceScreenSlot, 0, ContainerInput.PICKUP, player);
        client.gameMode.handleContainerInput(menu.containerId, targetScreenSlot, 0, ContainerInput.PICKUP, player);
        client.gameMode.handleContainerInput(menu.containerId, sourceScreenSlot, 0, ContainerInput.PICKUP, player);
    }

    private static int findRestockSource(Inventory inventory, ItemStack template, int excludedInventorySlot) {
        int bestSlot = -1;
        int bestCount = -1;
        int oppositeHandSlot = excludedInventorySlot == Inventory.SLOT_OFFHAND ? inventory.getSelectedSlot() : Inventory.SLOT_OFFHAND;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (slot == excludedInventorySlot || slot == oppositeHandSlot) {
                continue;
            }

            ItemStack candidate = inventory.getItem(slot);
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
        return ItemStack.isSameItemSameComponents(candidate.copyWithCount(1), template.copyWithCount(1));
    }

    private static int getHandInventorySlot(Inventory inventory, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? inventory.getSelectedSlot() : Inventory.SLOT_OFFHAND;
    }

    private static int toScreenSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < HOTBAR_SIZE) {
            return HOTBAR_SCREEN_SLOT_OFFSET + inventorySlot;
        }

        if (inventorySlot >= HOTBAR_SIZE && inventorySlot < 36) {
            return inventorySlot;
        }

        if (inventorySlot == Inventory.SLOT_OFFHAND) {
            return OFFHAND_SCREEN_SLOT;
        }

        return -1;
    }

    private static final class PendingUse {
        private final InteractionHand hand;
        private ItemStack capturedTemplate = ItemStack.EMPTY;
        private ItemStack template = ItemStack.EMPTY;
        private int ticksRemaining;

        private PendingUse(InteractionHand hand) {
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

        private void tick(Minecraft client, LocalPlayer player) {
            if (this.template.isEmpty()) {
                return;
            }

            ItemStack current = player.getItemInHand(this.hand);
            if (didConsumeFromHand(current)) {
                restockHand(client, player, this.hand, this.template);
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

        private boolean isStillUsing(LocalPlayer player, ItemStack current) {
            return player.isUsingItem()
                    && player.getUsedItemHand() == this.hand
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

    /**
     * Returns and clears the item stack captured by the last use attempt on the
     * given hand, or {@link ItemStack#EMPTY} if none was captured.
     *
     * @param hand the hand whose captured use should be consumed
     * @return the captured stack, or an empty stack
     */
    public static ItemStack consumeCapturedUse(InteractionHand hand) {
        PendingUse pendingUse = getPendingUse(hand);
        ItemStack captured = pendingUse.capturedTemplate;
        pendingUse.capturedTemplate = ItemStack.EMPTY;
        return captured;
    }
}