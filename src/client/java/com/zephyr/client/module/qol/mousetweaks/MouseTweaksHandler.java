package com.zephyr.client.module.qol.mousetweaks;

import com.zephyr.client.mixin.qol.MouseTweaks.AbstractContainerScreenAccessor;
import com.zephyr.client.module.qol.MouseTweaks;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Core MouseTweaks logic ported from YaLTeR's MouseTweaks Main.java.
 * Handles RMB, LMB and Wheel tweaks for AbstractContainerScreen.
 */
public final class MouseTweaksHandler {
    private static Screen openScreen = null;
    private static Slot oldSelectedSlot = null;
    private static double accumulatedScrollDelta = 0;
    private static boolean canDoLMBDrag = false;
    private static boolean canDoRMBDrag = false;
    private static boolean rmbTweakLeftOriginalSlot = false;

    private MouseTweaksHandler() {}

    private static void updateScreen(Screen newScreen) {
        if (newScreen == openScreen) return;
        openScreen = newScreen;
        oldSelectedSlot = null;
        accumulatedScrollDelta = 0;
        canDoLMBDrag = false;
        canDoRMBDrag = false;
        rmbTweakLeftOriginalSlot = false;
    }

    private static Slot getSlotUnderMouse(AbstractContainerScreen<?> screen, double x, double y) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        return accessor.zephyr$invokeGetHoveredSlot(x, y);
    }

    private static List<Slot> getSlots(AbstractContainerScreen<?> screen) {
        return screen.getMenu().slots;
    }

    private static void clickSlot(AbstractContainerScreen<?> screen, Slot slot, int button, boolean shift) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        ContainerInput input = shift ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP;
        accessor.zephyr$invokeSlotClicked(slot, slot.index, button, input);
    }

    private static boolean disableRMBDraggingFunctionality(AbstractContainerScreen<?> screen) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        accessor.zephyr$setSkipNextRelease(true);
        if (accessor.zephyr$getIsQuickCrafting() && accessor.zephyr$getQuickCraftingButton() == 1) {
            accessor.zephyr$setIsQuickCrafting(false);
            return true;
        }
        return false;
    }

    private static boolean isCraftingOutput(Slot slot) {
        return slot instanceof ResultSlot || slot instanceof FurnaceResultSlot || slot instanceof MerchantResultSlot;
    }

    private static boolean isIgnored(Slot slot) {
        return false;
    }

    private static boolean areStacksCompatible(ItemStack a, ItemStack b) {
        return a.isEmpty() || b.isEmpty() || (ItemStack.isSameItem(a, b) && ItemStack.isSameItemSameComponents(a, b));
    }

    private static boolean isShiftDown() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return false;
        return InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    // RMB helper
    private static void rmbTweakMaybeClickSlot(AbstractContainerScreen<?> screen, Slot slot, ItemStack stackOnMouse) {
        if (slot == null) return;
        if (stackOnMouse.isEmpty()) return;
        if (isIgnored(slot)) return;
        if (isCraftingOutput(slot)) return;
        if (!(stackOnMouse.getItem() instanceof BundleItem)) {
            ItemStack slotStack = slot.getItem();
            if (!areStacksCompatible(slotStack, stackOnMouse)) return;
            if (slotStack.getCount() == slot.getMaxStackSize(slotStack)) return;
        }
        clickSlot(screen, slot, 1, false);
    }

    public static boolean onMouseClicked(AbstractContainerScreen<?> screen, double x, double y, int button) {
        updateScreen(screen);
        if (!MouseTweaks.INSTANCE.isEnabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        Slot slotUnderMouse = getSlotUnderMouse(screen, x, y);
        oldSelectedSlot = slotUnderMouse;

        ItemStack stackOnMouse = mc.player.containerMenu.getCarried();
        if (button == 0) { // LEFT
            if (stackOnMouse.isEmpty()) {
                canDoLMBDrag = true;
            }
        } else if (button == 1) { // RIGHT
            if (stackOnMouse.isEmpty()) return false;
            if (!MouseTweaks.INSTANCE.rmbTweakEnabled()) return false;
            canDoRMBDrag = true;
            rmbTweakLeftOriginalSlot = false;
        }
        return false;
    }

    public static boolean onMouseReleased(AbstractContainerScreen<?> screen, double x, double y, int button) {
        updateScreen(screen);
        if (!MouseTweaks.INSTANCE.isEnabled()) return false;
        if (button == 0) canDoLMBDrag = false;
        else if (button == 1) canDoRMBDrag = false;
        return false;
    }

    public static boolean onMouseDragged(AbstractContainerScreen<?> screen, double x, double y, int button) {
        updateScreen(screen);
        if (!MouseTweaks.INSTANCE.isEnabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        Slot selectedSlot = getSlotUnderMouse(screen, x, y);
        if (selectedSlot == oldSelectedSlot) return false;

        ItemStack stackOnMouse = mc.player.containerMenu.getCarried();

        if (canDoRMBDrag && button == 1 && !rmbTweakLeftOriginalSlot) {
            rmbTweakLeftOriginalSlot = true;
            disableRMBDraggingFunctionality(screen);
            rmbTweakMaybeClickSlot(screen, oldSelectedSlot, stackOnMouse);
        }

        oldSelectedSlot = selectedSlot;

        if (selectedSlot == null) return false;
        if (isIgnored(selectedSlot)) return false;

        if (button == 0) { // LEFT
            if (!canDoLMBDrag) return false;
            ItemStack slotStack = selectedSlot.getItem();
            if (slotStack.isEmpty()) return false;
            boolean shiftIsDown = isShiftDown();

            if (stackOnMouse.isEmpty()) {
                if (!MouseTweaks.INSTANCE.lmbTweakWithoutItemEnabled() || !shiftIsDown) return false;
                clickSlot(screen, selectedSlot, 0, true);
            } else {
                if (!MouseTweaks.INSTANCE.lmbTweakWithItemEnabled()) return false;
                if (!areStacksCompatible(slotStack, stackOnMouse)) return false;
                if (shiftIsDown) {
                    clickSlot(screen, selectedSlot, 0, true);
                } else {
                    if (stackOnMouse.getCount() + slotStack.getCount() > stackOnMouse.getMaxStackSize()) return false;
                    clickSlot(screen, selectedSlot, 0, false);
                    if (!isCraftingOutput(selectedSlot)) {
                        clickSlot(screen, selectedSlot, 0, false);
                    }
                }
            }
        } else if (button == 1) { // RIGHT
            if (!canDoRMBDrag) return false;
            rmbTweakMaybeClickSlot(screen, selectedSlot, stackOnMouse);
        }
        return false;
    }

    public static boolean onMouseScrolled(AbstractContainerScreen<?> screen, double x, double y, double scrollDelta) {
        updateScreen(screen);
        if (!MouseTweaks.INSTANCE.isEnabled() || !MouseTweaks.INSTANCE.wheelTweakEnabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        Slot selectedSlot = getSlotUnderMouse(screen, x, y);
        if (selectedSlot == null || isIgnored(selectedSlot)) return false;

        ItemStack selectedSlotStack = selectedSlot.getItem();

        // Always handle scroll above valid slot to prevent passing through to other handlers
        double scaledDelta = scrollDelta; // 1 item per scroll step
        if (accumulatedScrollDelta != 0 && Math.signum(scaledDelta) != Math.signum(accumulatedScrollDelta)) {
            accumulatedScrollDelta = 0;
        }
        accumulatedScrollDelta += scaledDelta;
        int delta = (int) accumulatedScrollDelta;
        accumulatedScrollDelta -= (double) delta;
        if (delta == 0) return true;

        List<Slot> slots = getSlots(screen);
        int numItemsToMove = Math.abs(delta);
        boolean pushItems = (delta < 0);
        if (MouseTweaks.INSTANCE.isWheelInverted()) {
            pushItems = !pushItems;
        }

        if (selectedSlotStack.isEmpty()) return true;

        ItemStack stackOnMouse = mc.player.containerMenu.getCarried();

        // Crafting output handling
        if (isCraftingOutput(selectedSlot)) {
            if (!areStacksCompatible(selectedSlotStack, stackOnMouse)) return true;
            if (stackOnMouse.isEmpty()) {
                if (!pushItems) return true;
                while (numItemsToMove-- > 0) {
                    List<Slot> targetSlots = findPushSlots(slots, selectedSlot, selectedSlotStack.getCount(), true);
                    if (targetSlots == null) break;
                    clickSlot(screen, selectedSlot, 0, false);
                    for (int i = 0; i < targetSlots.size(); i++) {
                        Slot slot = targetSlots.get(i);
                        if (i == targetSlots.size() - 1) {
                            clickSlot(screen, slot, 0, false);
                        } else {
                            int clickTimes = slot.getMaxStackSize(slot.getItem()) - slot.getItem().getCount();
                            while (clickTimes-- > 0) clickSlot(screen, slot, 1, false);
                        }
                    }
                }
            } else {
                while (numItemsToMove-- > 0) clickSlot(screen, selectedSlot, 0, false);
            }
            return true;
        }

        if (!stackOnMouse.isEmpty() && areStacksCompatible(selectedSlotStack, stackOnMouse)) return true;

        if (pushItems) {
            if (!stackOnMouse.isEmpty() && !selectedSlot.mayPlace(stackOnMouse)) return true;
            numItemsToMove = Math.min(numItemsToMove, selectedSlotStack.getCount());
            List<Slot> targetSlots = findPushSlots(slots, selectedSlot, numItemsToMove, false);
            if (targetSlots == null) return true;
            if (targetSlots.isEmpty()) return true;

            boolean hadItemOnMouse = !stackOnMouse.isEmpty();
            int pickUpButton = 1; // RIGHT
            if (stackOnMouse.isEmpty() && selectedSlotStack.getCount() <= numItemsToMove) pickUpButton = 0;
            clickSlot(screen, selectedSlot, pickUpButton, false);

            ItemStack pickedUpStack = mc.player.containerMenu.getCarried();
            numItemsToMove = Math.min(numItemsToMove, pickedUpStack.getCount());

            for (Slot slot : targetSlots) {
                int clickTimes = slot.getMaxStackSize(pickedUpStack) - slot.getItem().getCount();
                clickTimes = Math.min(clickTimes, numItemsToMove);
                numItemsToMove -= clickTimes;
                while (clickTimes-- > 0) clickSlot(screen, slot, 1, false);
            }

            boolean hasLeftoverItems = !mc.player.containerMenu.getCarried().isEmpty();
            if (hadItemOnMouse || hasLeftoverItems) {
                int putDownButton = 0;
                if (hadItemOnMouse && hasLeftoverItems) putDownButton = 1;
                clickSlot(screen, selectedSlot, putDownButton, false);
            }
            return true;
        }

        // pull
        int maxItemsToMove = selectedSlot.getMaxStackSize(selectedSlotStack) - selectedSlotStack.getCount();
        numItemsToMove = Math.min(numItemsToMove, maxItemsToMove);

        while (numItemsToMove > 0) {
            Slot targetSlot = findPullSlot(slots, selectedSlot);
            if (targetSlot == null) break;
            ItemStack targetSlotStack = targetSlot.getItem();
            int numItemsInTargetSlot = targetSlotStack.getCount();

            if (isCraftingOutput(targetSlot)) {
                if (maxItemsToMove < numItemsInTargetSlot) break;
                maxItemsToMove -= numItemsInTargetSlot;
                numItemsToMove = Math.min(numItemsToMove - 1, maxItemsToMove);
                if (!stackOnMouse.isEmpty() && !selectedSlot.mayPlace(stackOnMouse)) break;
                clickSlot(screen, selectedSlot, 0, false);
                clickSlot(screen, targetSlot, 0, false);
                clickSlot(screen, selectedSlot, 0, false);
                continue;
            }

            boolean hadItemOnMouse = !stackOnMouse.isEmpty();
            if (hadItemOnMouse && !targetSlot.mayPlace(stackOnMouse)) break;

            int pickUpButton = 1;
            if (stackOnMouse.isEmpty() && targetSlotStack.getCount() == 1) pickUpButton = 0;
            clickSlot(screen, targetSlot, pickUpButton, false);

            int numPickedUp = mc.player.containerMenu.getCarried().getCount();
            int numItemsToMoveFromTargetSlot = Math.min(numPickedUp, numItemsToMove);
            if (numItemsToMoveFromTargetSlot == numPickedUp) {
                clickSlot(screen, selectedSlot, 0, false);
            } else {
                for (int i = 0; i < numItemsToMoveFromTargetSlot; i++) clickSlot(screen, selectedSlot, 1, false);
            }
            maxItemsToMove -= numItemsToMoveFromTargetSlot;
            numItemsToMove -= numItemsToMoveFromTargetSlot;

            boolean hasLeftoverItems = !mc.player.containerMenu.getCarried().isEmpty();
            if (hadItemOnMouse || hasLeftoverItems) {
                int putDownButton = 0;
                if (hadItemOnMouse && hasLeftoverItems) putDownButton = 1;
                clickSlot(screen, targetSlot, putDownButton, false);
            }
        }
        return true;
    }

    private static Slot findPullSlot(List<Slot> slots, Slot selectedSlot) {
        Minecraft mc = Minecraft.getInstance();
        int startIndex, endIndex, direction;
        if (MouseTweaks.INSTANCE.wheelSearchOrder() == MouseTweaks.WheelSearchOrder.FIRST_TO_LAST) {
            startIndex = 0;
            endIndex = slots.size();
            direction = 1;
        } else {
            startIndex = slots.size() - 1;
            endIndex = -1;
            direction = -1;
        }
        ItemStack selectedSlotStack = selectedSlot.getItem();
        boolean findInPlayerInventory = (selectedSlot.container != mc.player.getInventory());

        for (int i = startIndex; i != endIndex; i += direction) {
            Slot slot = slots.get(i);
            if (isIgnored(slot)) continue;
            boolean slotInPlayerInventory = (slot.container == mc.player.getInventory());
            if (findInPlayerInventory != slotInPlayerInventory) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            if (!areStacksCompatible(selectedSlotStack, stack)) continue;
            return slot;
        }
        return null;
    }

    private static List<Slot> findPushSlots(List<Slot> slots, Slot selectedSlot, int itemCount, boolean mustDistributeAll) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack selectedSlotStack = selectedSlot.getItem();
        boolean findInPlayerInventory = (selectedSlot.container != mc.player.getInventory());
        List<Slot> rv = new ArrayList<>();
        List<Slot> goodEmptySlots = new ArrayList<>();

        for (int i = 0; i != slots.size() && itemCount > 0; i++) {
            Slot slot = slots.get(i);
            if (isIgnored(slot)) continue;
            boolean slotInPlayerInventory = (slot.container == mc.player.getInventory());
            if (findInPlayerInventory != slotInPlayerInventory) continue;
            if (isCraftingOutput(slot)) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                if (slot.mayPlace(selectedSlotStack)) goodEmptySlots.add(slot);
            } else {
                if (areStacksCompatible(selectedSlotStack, stack) && stack.getCount() < slot.getMaxStackSize(stack)) {
                    rv.add(slot);
                    itemCount -= Math.min(itemCount, slot.getMaxStackSize(stack) - stack.getCount());
                }
            }
        }

        for (int i = 0; i != goodEmptySlots.size() && itemCount > 0; i++) {
            Slot slot = goodEmptySlots.get(i);
            rv.add(slot);
            itemCount -= Math.min(itemCount, slot.getMaxStackSize(selectedSlotStack));
        }

        if (mustDistributeAll && itemCount > 0) return null;
        return rv;
    }
}
