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

    public InventoryAwarePreviewProvider(int maxRowSize, Supplier<? extends I> inventoryFactory) {
        super(27, false, maxRowSize, maxRowSize);
        this.inventoryFactory = inventoryFactory;
    }

    private I getInventory() {
        I inv = this.cachedInventory.get();
        if (inv == null) {
            inv = this.inventoryFactory.get();
            this.cachedInventory.set(inv);
        }
        return inv;
    }

    @Override
    public boolean showTooltipHints(PreviewContext context) {
        return this.shouldDisplay(context);
    }

    @Override
    public int getInventoryMaxSize(PreviewContext context) {
        return this.getInventory().getContainerSize();
    }

    @Override
    public boolean canUseLootTables() {
        return this.getInventory() instanceof RandomizableContainer;
    }
}
