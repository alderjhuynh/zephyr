package com.zephyr.client.module.qol.jade.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for everything that can be placed inside a Jade tooltip line.
 * Elements are laid out left-to-right within a {@link BoxElement}; each element
 * knows its own size and how to draw itself. The {@link #alpha} field lets the
 * containing box fade the whole tooltip in/out uniformly.
 *
 * <p>1.21.1 port: draws with {@link GuiGraphics} directly instead of queuing
 * through {@code GuiGraphicsExtractor}; the unused mouse/partial-tick parameters
 * are dropped.
 */
public abstract class Element {
    public int x;
    public int y;
    public int width;
    public int height;
    public float alpha = 1.0F;

    /**
     * Draws the element at its current {@link #x}/{@link #y} position.
     * The default implementation is a no-op; subclasses override it to draw
     * themselves.
     *
     * @param graphics the graphics context to draw into
     */
    public void render(GuiGraphics graphics) {
    }

    /** @return the shared Minecraft font for measuring and drawing text elements */
    protected static Font font() {
        return Minecraft.getInstance().font;
    }
}
