package com.zephyr.client.module.qol.shulkerboxtooltip.render;

import com.zephyr.client.module.qol.ShulkerBoxTooltip;
import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewContext;
import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewProvider;
import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewType;
import com.zephyr.client.module.qol.shulkerboxtooltip.TooltipType;

import org.jetbrains.annotations.NotNull;

/**
 * Renders a preview using a {@link PreviewProvider}.
 */
public interface PreviewRenderer {
    /**
     * Returns the renderer matching the module's tooltip type setting.
     */
    @NotNull
    static PreviewRenderer getDefaultRendererInstance() {
        return ShulkerBoxTooltip.INSTANCE.tooltipType() == TooltipType.VANILLA
                ? VanillaPreviewRenderer.INSTANCE
                : ModPreviewRenderer.INSTANCE;
    }

    /**
     * Gets the pixel height of the preview window.
     */
    int getHeight();

    /**
     * Gets the pixel width of the preview window.
     */
    int getWidth();

    /**
     * Sets the preview to use for the given context.
     */
    void setPreview(PreviewContext context, PreviewProvider provider);

    /**
     * Sets the preview type.
     */
    void setPreviewType(PreviewType type);

    /**
     * Renders the preview.
     */
    void draw(RenderContext context);
}
