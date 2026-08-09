package com.zephyr.client.module.qol.shulkerboxtooltip.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public record RenderContextImpl(int x, int y, int viewportWidth, int viewportHeight, GuiGraphics graphics,
                                Font font, int mouseX, int mouseY, int tooltipTopX, int tooltipTopY)
        implements RenderContext {
}
