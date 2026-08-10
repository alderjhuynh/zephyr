package com.zephyr.client.module.qol.shulkerboxtooltip;

import com.zephyr.client.module.qol.shulkerboxtooltip.render.PreviewRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Describes preview properties for a registered set of items.
 */
public interface PreviewProvider {
    /**
     * Queries if the preview window should be displayed for the given context.
     * Should return {@code false} if the inventory is empty.
     */
    boolean shouldDisplay(PreviewContext context);

    /**
     * Fetches the items to be displayed in the preview.
     */
    List<ItemStack> getInventory(PreviewContext context);

    /**
     * Returns the maximum amount of slots this preview can display.
     */
    int getInventoryMaxSize(PreviewContext context);

    /**
     * The maximum number of item stacks to be displayed in a row in full preview mode.
     */
    default int getMaxRowSize(PreviewContext context) {
        return 0;
    }

    /**
     * The maximum number of item stacks to be displayed in a row in compact preview mode.
     */
    default int getCompactMaxRowSize(PreviewContext context) {
        return 0;
    }

    /**
     * Returns whether this provider supports full preview mode.
     */
    default boolean isFullPreviewAvailable(PreviewContext context) {
        return true;
    }

    /**
     * Should hints be shown in the item's tooltip?
     */
    default boolean showTooltipHints(PreviewContext context) {
        return true;
    }

    /**
     * Which color the preview window should be in.
     */
    default ColorKey getWindowColorKey(PreviewContext context) {
        return ColorKey.DEFAULT;
    }

    /**
     * The identifier of the texture used to display the preview window, or {@code null} for
     * the default texture.
     */
    @Nullable
    default ResourceLocation getTextureOverride(PreviewContext context) {
        return null;
    }

    /**
     * The renderer to use for this type of preview.
     */
    default PreviewRenderer getRenderer() {
        return PreviewRenderer.getDefaultRendererInstance();
    }

    /**
     * Adds lines to the stack tooltip. Returned lines are added only if the tooltip type is
     * set to {@code MOD} in the module settings.
     */
    default List<Component> addTooltip(PreviewContext context) {
        return Collections.emptyList();
    }
}
