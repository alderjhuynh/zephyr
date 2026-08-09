package com.zephyr.client.module.qol.shulkerboxtooltip;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps container items to their {@link PreviewProvider}.
 */
public final class PreviewProviderRegistry {
    private static final PreviewProviderRegistry INSTANCE = new PreviewProviderRegistry();

    private final Map<Item, PreviewProvider> providerItems = new HashMap<>();

    private PreviewProviderRegistry() {
    }

    public static PreviewProviderRegistry getInstance() {
        return INSTANCE;
    }

    public void register(PreviewProvider provider, Item... items) {
        for (Item item : items) {
            this.providerItems.put(item, provider);
        }
    }

    public void register(PreviewProvider provider, Iterable<Item> items) {
        for (Item item : items) {
            this.providerItems.put(item, provider);
        }
    }

    @Nullable
    public PreviewProvider get(ItemStack stack) {
        return this.providerItems.get(stack.getItem());
    }

    @Nullable
    public PreviewProvider get(Item item) {
        return this.providerItems.get(item);
    }
}
