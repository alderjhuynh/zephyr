package com.zephyr.client.module.qol.jade.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for everything that can be placed inside a Jade tooltip line.
 * Elements are laid out left-to-right within a {@link BoxElement}; each element
 * knows its own size and how to draw itself. The {@link #alpha} field lets the
 * containing box fade the whole tooltip in/out uniformly.
 */
public abstract class Element {
    public int x;
    public int y;
    public int width;
    public int height;
    public float alpha = 1.0F;

    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    }

    protected static Font font() {
        return Minecraft.getInstance().font;
    }
}
