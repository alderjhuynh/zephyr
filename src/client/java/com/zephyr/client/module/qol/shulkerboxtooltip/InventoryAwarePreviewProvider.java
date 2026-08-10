package com.zephyr.client.module.qol.shulkerboxtooltip;

import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;

import java.util.function.Supplier;

/**
 * A {@link BlockEntityPreviewProvider} that uses a {@link Container} instance to determine
 * the inventory size of the item it previews.
 */
public class InventoryAwarePreviewProvider<I extends Container> extends BlockEntityPreviewProvider {

    private final Supplier<? extends I> inventoryFactory;

    private final ThreadLocal<I> cachedInventory = ThreadLocal.withInitial(() -> null);

    /**
     * Creates a provider whose inventory size comes from a lazily-created,
     * thread-locally cached {@link Container}.
     *
     * @param maxRowSize        the number of slots per preview row
     * @param inventoryFactory  supplies the backing container
     */
    public InventoryAwarePreviewProvider(int maxRowSize, Supplier<? extends I> inventoryFactory) {
        super(27, false, maxRowSize, maxRowSize);
        this.inventoryFactory = inventoryFactory;
    }

    /**
     * Returns the cached container, creating it once per thread on first use.
     *
     * @return the backing container instance
     */
    private I getInventory() {
        I inv = this.cachedInventory.get();
        if (inv == null) {
            inv = this.inventoryFactory.get();
            this.cachedInventory.set(inv);
        }
        return inv;
    }

    /** @return whether the item's contents should be previewed at all */
    @Override
    public boolean showTooltipHints(PreviewContext context) {
        return this.shouldDisplay(context);
    }

    /** @return the container's slot count */
    @Override
    public int getInventoryMaxSize(PreviewContext context) {
        return this.getInventory().getContainerSize();
    }

    /** @return whether the backing container supports loot tables */
    @Override
    public boolean canUseLootTables() {
        return this.getInventory() instanceof RandomizableContainer;
    }
}
