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

    public MergedItemStack(int slotCount) {
        this.merged = ItemStack.EMPTY;
        this.subItems = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        this.firstSlot = Integer.MAX_VALUE;
    }

    public ItemStack get() {
        return this.merged;
    }

    /**
     * Add the passed stack into the item list. Does not check if items are equal.
     *
     * @param stack The stack to add
     * @param slot  The slot this stack is located in.
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

    public ItemStack getSubStack(int slot) {
        if (slot < 0 || slot >= this.subItems.size())
            return ItemStack.EMPTY;
        return this.subItems.get(slot);
    }

    public int getFirstSlot() {
        return this.firstSlot;
    }

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
