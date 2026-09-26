package com.zephyr.client.module.qol.shulkerboxtooltip.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Groups identical item stacks together for compact preview rendering.
 */
public class MergedItemStack implements Comparable<MergedItemStack> {
    private ItemStack merged;
    private final NonNullList<ItemStack> subItems;
    private int firstSlot;

    /**
     * Creates an empty merged group sized to the inventory slot count.
     *
     * @param slotCount the number of inventory slots to track sub-stacks for
     */
    public MergedItemStack(int slotCount) {
        this.merged = ItemStack.EMPTY;
        this.subItems = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        this.firstSlot = Integer.MAX_VALUE;
    }

    /** @return the merged stack aggregating the grouped stacks' counts */
    public ItemStack get() {
        return this.merged;
    }

    /**
     * Add the passed stack into the item list. Does not check if items are equal.
     *
     * @param stack The stack to add
     * @param slot  The slot this stack is located in.
     * @param ignoreComponents Whether component differences are ignored when merging
     */
    public void add(ItemStack stack, int slot, boolean ignoreComponents) {
        if (slot < 0 || slot >= this.subItems.size())
            return;
        this.subItems.set(slot, stack.copy());
        if (slot < this.firstSlot)
            this.firstSlot = slot;
        if (this.merged.isEmpty()) {
            if (ignoreComponents) {
                this.merged = copyStackWithoutComponents(stack);
            } else {
                this.merged = stack.copy();
            }
        } else {
            this.merged.grow(stack.getCount());
        }
    }

    private static ItemStack copyStackWithoutComponents(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            var copy = new ItemStack(stack.getItem(), stack.getCount());
            copy.setPopTime(stack.getPopTime());
            return copy;
        }
    }

    /** @return the stack in the given slot, or {@link ItemStack#EMPTY} if out of bounds */
    public ItemStack getSubStack(int slot) {
        if (slot < 0 || slot >= this.subItems.size())
            return ItemStack.EMPTY;
        return this.subItems.get(slot);
    }

    /** @return the first inventory slot this group appeared in */
    public int getFirstSlot() {
        return this.firstSlot;
    }

    /**
     * Orders groups by merged count descending, then by first-slot ascending.
     *
     * @param other the group to compare against
     * @return a negative, zero, or positive value per the ordering
     */
    @Override
    public int compareTo(MergedItemStack other) {
        int ret = this.merged.getCount() - other.merged.getCount();

        if (ret != 0)
            return ret;
        return other.firstSlot - this.firstSlot;
    }

    /**
     * Merges the given inventory into a list of grouped stacks, sorted by stack size
     * (largest first), then by the slot order they first appear in.
     */
    public static List<MergedItemStack> mergeInventory(List<ItemStack> inventory, int maxSize,
                                                       boolean ignoreComponents) {
        var items = new ArrayList<MergedItemStack>();

        if (!inventory.isEmpty()) {
            var mergedStacks = new HashMap<ItemKey, MergedItemStack>();

            for (int i = 0, len = inventory.size(); i < len; ++i) {
                ItemStack s = inventory.get(i);

                if (s.isEmpty())
                    continue;

                ItemKey k = new ItemKey(s, ignoreComponents);
                MergedItemStack mergedStack = mergedStacks.get(k);

                if (mergedStack == null) {
                    mergedStack = new MergedItemStack(maxSize);
                    mergedStacks.put(k, mergedStack);
                }
                mergedStack.add(s, i, ignoreComponents);
            }

            items.addAll(mergedStacks.values());
            items.sort(Comparator.reverseOrder());
        }
        return items;
    }
}
