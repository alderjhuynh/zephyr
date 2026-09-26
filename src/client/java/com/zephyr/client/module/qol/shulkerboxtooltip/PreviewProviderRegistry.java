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

    /** Private singleton constructor. */
    private PreviewProviderRegistry() {
    }

    /** @return the shared provider registry instance */
    public static PreviewProviderRegistry getInstance() {
        return INSTANCE;
    }

    /** Registers a provider for every item in the given array. */
    public void register(PreviewProvider provider, Item... items) {
        for (Item item : items) {
            this.providerItems.put(item, provider);
        }
    }

    /** Registers a provider for every item in the given iterable. */
    public void register(PreviewProvider provider, Iterable<Item> items) {
        for (Item item : items) {
            this.providerItems.put(item, provider);
        }
    }

    /** @return the provider registered for the stack's item, or {@code null} */
    @Nullable
    public PreviewProvider get(ItemStack stack) {
        return this.providerItems.get(stack.getItem());
    }

    /** @return the provider registered for the item, or {@code null} */
    @Nullable
    public PreviewProvider get(Item item) {
        return this.providerItems.get(item);
    }
}
