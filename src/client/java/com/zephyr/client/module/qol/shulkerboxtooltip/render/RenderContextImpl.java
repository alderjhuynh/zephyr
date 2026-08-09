package com.zephyr.client.module.qol.shulkerboxtooltip.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public record RenderContextImpl(int x, int y, int viewportWidth, int viewportHeight, GuiGraphicsExtractor graphics,
                                Font font, int mouseX, int mouseY, int tooltipTopX, int tooltipTopY)
        implements RenderContext {
}
