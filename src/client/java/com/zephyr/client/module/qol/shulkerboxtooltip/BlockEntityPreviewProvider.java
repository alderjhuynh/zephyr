package com.zephyr.client.module.qol.shulkerboxtooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link PreviewProvider} that works on items carrying block entity data, i.e. items whose
 * contents are stored in the {@code DataComponents.CONTAINER} component (as created by
 * {@code ContainerHelper.saveAllItems}).
 */
public class BlockEntityPreviewProvider implements PreviewProvider {
    private final int defaultMaxInvSize;
    private final boolean defaultCanUseLootTables;
    private final int defaultMaxRowSize;
    private final int defaultCompactMaxRowSize;

    public BlockEntityPreviewProvider(int defaultMaxInvSize, boolean defaultCanUseLootTables, int defaultMaxRowSize,
                                      int defaultCompactMaxRowSize) {
        this.defaultMaxInvSize = defaultMaxInvSize;
        this.defaultCanUseLootTables = defaultCanUseLootTables;
        this.defaultMaxRowSize = defaultMaxRowSize <= 0 ? 9 : defaultMaxRowSize;
        this.defaultCompactMaxRowSize = defaultCompactMaxRowSize;
    }

    @Override
    public boolean shouldDisplay(PreviewContext context) {
        if (this.canUseLootTables() && context.stack().has(DataComponents.CONTAINER_LOOT))
            return false;
        return getItemCount(this.getInventory(context)) > 0;
    }

    @Override
    public boolean showTooltipHints(PreviewContext context) {
        return context.stack().has(DataComponents.CONTAINER);
    }

    @Override
    public List<ItemStack> getInventory(PreviewContext context) {
        var registries = context.registryLookup();
        var container = context.stack().get(DataComponents.CONTAINER);
        var invMaxSize = this.getInventoryMaxSize(context);
        var inv = NonNullList.withSize(invMaxSize, ItemStack.EMPTY);

        if (registries != null && container != null)
            container.copyInto(inv);

        return inv;
    }

    @Override
    public int getInventoryMaxSize(PreviewContext context) {
        return this.defaultMaxInvSize;
    }

    @Override
    public List<Component> addTooltip(PreviewContext context) {
        if (ShulkerBoxTooltipApi.getCurrentPreviewType(this.isFullPreviewAvailable(context)) == PreviewType.FULL)
            return Collections.emptyList();
        return getItemListTooltip(new ArrayList<>(), this.getInventory(context),
                Style.EMPTY.withColor(ChatFormatting.GRAY));
    }

    /**
     * Adds a line stating the number of items in the passed inventory, or 'empty' if it has none.
     */
    public static List<Component> getItemListTooltip(List<Component> tooltip, @Nullable List<ItemStack> items,
                                                     Style style) {
        int itemCount = getItemCount(items);
        MutableComponent text;

        if (itemCount > 0)
            text = Component.translatable("container.shulkerbox.contains", itemCount);
        else
            text = Component.translatable("container.shulkerbox.empty");
        tooltip.add(text.setStyle(style));
        return tooltip;
    }

    @Override
    public int getMaxRowSize(PreviewContext context) {
        return this.defaultMaxRowSize;
    }

    @Override
    public int getCompactMaxRowSize(PreviewContext context) {
        return this.defaultCompactMaxRowSize;
    }

    /**
     * If true, previews will not be shown when a loot table component is present.
     */
    public boolean canUseLootTables() {
        return this.defaultCanUseLootTables;
    }

    private static int getItemCount(@Nullable List<ItemStack> items) {
        int itemCount = 0;

        if (items != null)
            for (ItemStack stack : items)
                if (stack.getItem() != Items.AIR)
                    ++itemCount;
        return itemCount;
    }
}
