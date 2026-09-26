package com.zephyr.client.module.qol.shulkerboxtooltip.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Default {@link RenderContext} implementation carrying all render parameters as
 * an immutable record.
 */
public record RenderContextImpl(int x, int y, int viewportWidth, int viewportHeight, GuiGraphicsExtractor graphics,
                                Font font, int mouseX, int mouseY, int tooltipTopX, int tooltipTopY)
        implements RenderContext {
}
