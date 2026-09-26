package com.zephyr.client.module.qol.jade.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

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

    /**
     * Extracts and queues the element's render state at its current
     * {@link #x}/{@link #y} position. The default implementation is a no-op;
     * subclasses override it to draw themselves.
     *
     * @param graphics      the graphics context to draw into
     * @param mouseX        the current mouse X in GUI pixels
     * @param mouseY        the current mouse Y in GUI pixels
     * @param partialTicks  the partial tick time
     */
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
    }

    /** @return the shared Minecraft font for measuring and drawing text elements */
    protected static Font font() {
        return Minecraft.getInstance().font;
    }
}
