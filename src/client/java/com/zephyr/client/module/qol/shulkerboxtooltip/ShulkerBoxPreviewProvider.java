package com.zephyr.client.module.qol.shulkerboxtooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Preview provider for shulker box items.
 */
public class ShulkerBoxPreviewProvider extends InventoryAwarePreviewProvider<ShulkerBoxBlockEntity> {
    /**
     * Creates a shulker box preview provider.
     *
     * @param maxRowSize           the number of slots per preview row
     * @param blockEntitySupplier  supplies the block entity template backing the preview
     */
    public ShulkerBoxPreviewProvider(int maxRowSize, Supplier<? extends ShulkerBoxBlockEntity> blockEntitySupplier) {
        super(maxRowSize, blockEntitySupplier);
    }

    /** Shulker boxes always advertise tooltip hints. */
    @Override
    public boolean showTooltipHints(PreviewContext context) {
        return true;
    }

    /** @return the window color key derived from the box's dye color */
    @Override
    public ColorKey getWindowColorKey(PreviewContext context) {
        DyeColor dye = ((ShulkerBoxBlock) Block.byItem(context.stack().getItem())).getColor();

        if (dye == null)
            return ColorKey.SHULKER_BOX;
        return switch (dye) {
            case ORANGE -> ColorKey.ORANGE_SHULKER_BOX;
            case MAGENTA -> ColorKey.MAGENTA_SHULKER_BOX;
            case LIGHT_BLUE -> ColorKey.LIGHT_BLUE_SHULKER_BOX;
            case YELLOW -> ColorKey.YELLOW_SHULKER_BOX;
            case LIME -> ColorKey.LIME_SHULKER_BOX;
            case PINK -> ColorKey.PINK_SHULKER_BOX;
            case GRAY -> ColorKey.GRAY_SHULKER_BOX;
            case LIGHT_GRAY -> ColorKey.LIGHT_GRAY_SHULKER_BOX;
            case CYAN -> ColorKey.CYAN_SHULKER_BOX;
            case PURPLE -> ColorKey.PURPLE_SHULKER_BOX;
            case BLUE -> ColorKey.BLUE_SHULKER_BOX;
            case BROWN -> ColorKey.BROWN_SHULKER_BOX;
            case GREEN -> ColorKey.GREEN_SHULKER_BOX;
            case RED -> ColorKey.RED_SHULKER_BOX;
            case BLACK -> ColorKey.BLACK_SHULKER_BOX;
            default -> ColorKey.WHITE_SHULKER_BOX;
        };
    }

    /**
     * Adds the item-count line, or the vanilla "???????" hint when the box has a
     * loot table component.
     */
    @Override
    public List<Component> addTooltip(PreviewContext context) {
        ItemStack stack = context.stack();

        // Restore the vanilla behavior of adding question marks to the tooltip when the item has a loot table
        if (this.canUseLootTables() && stack.has(DataComponents.CONTAINER_LOOT)) {
            Style style = Style.EMPTY.withColor(ChatFormatting.GRAY);

            return Collections.singletonList(Component.literal("???????").setStyle(style));
        }
        return super.addTooltip(context);
    }
}
