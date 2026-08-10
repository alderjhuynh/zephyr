package com.zephyr.client.module.qol.shulkerboxtooltip.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import org.jetbrains.annotations.NotNull;

/**
 * Parameters of {@link PreviewRenderer#draw(RenderContext)}.
 */
public interface RenderContext {
    /**
     * X position of the preview's upper-left corner.
     */
    int x();

    /**
     * Y position of the preview's upper-left corner.
     */
    int y();

    /**
     * Number of pixels available for rendering the preview in the X axis.
     */
    int viewportWidth();

    /**
     * Number of pixels available for rendering the preview in the Y axis.
     */
    int viewportHeight();

    /**
     * GUI graphics extractor instance.
     */
    @NotNull
    GuiGraphics graphics();

    /**
     * The text renderer.
     */
    @NotNull
    Font font();

    /**
     * The X position of the mouse cursor, relative to the current active Screen.
     */
    int mouseX();

    /**
     * The Y position of the mouse cursor, relative to the current active Screen.
     */
    int mouseY();

    /**
     * X position of the tooltip's upper-left corner.
     */
    int tooltipTopX();

    /**
     * Y position of the tooltip's upper-left corner.
     */
    int tooltipTopY();
}
