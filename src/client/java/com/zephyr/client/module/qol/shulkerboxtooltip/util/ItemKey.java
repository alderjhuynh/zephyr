package com.zephyr.client.module.qol.shulkerboxtooltip.util;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Used as a key in maps to group identical item stacks together.
 */
public class ItemKey {
    private final Item item;
    private final int id;
    private final DataComponentMap components;
    private final boolean ignoreComponents;

    public ItemKey(ItemStack stack, boolean ignoreComponents) {
        this.item = stack.getItem();
        this.id = BuiltInRegistries.ITEM.getId(this.item);
        this.components = stack.getComponents();
        this.ignoreComponents = ignoreComponents;
    }

    @Override
    public int hashCode() {
        return 31 * id + (this.ignoreComponents || components == null ? 0 : components.hashCode());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof ItemKey key))
            return false;

        return key.item == this.item && key.id == this.id && (this.ignoreComponents || Objects.equals(key.components,
                this.components));
    }
}
