package com.zephyr.client.module.qol.jade.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * A single line of text inside the tooltip, colored like Jade's themed text
 * (title rows use the bright color, info rows the dimmer one, mod-name rows a
 * muted gray). The color's alpha is scaled by the element's {@link #alpha} so the
 * whole overlay fades in and out uniformly.
 */
public class TextElement extends Element {
    private final Component text;
    private final int color;
    private final boolean shadow;

    /**
     * Creates a text element measuring the component with the shared font.
     *
     * @param text   the text to render
     * @param color  the ARGB text color
     * @param shadow whether to draw a drop shadow behind the text
     */
    public TextElement(Component text, int color, boolean shadow) {
        this.text = text;
        this.color = color;
        this.shadow = shadow;
        this.width = font().width(text);
        this.height = font().lineHeight;
    }

    /** @return the text rendered by this element */
    public Component getText() {
        return text;
    }

    /** Draws the text at the element's current position with a scaled alpha. */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.text(font(), text, x, y, ARGB.multiplyAlpha(color, alpha), shadow);
    }
}
