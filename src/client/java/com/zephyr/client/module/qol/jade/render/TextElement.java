package com.zephyr.client.module.qol.jade.render;

import net.minecraft.client.gui.GuiGraphics;
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

    public TextElement(Component text, int color, boolean shadow) {
        this.text = text;
        this.color = color;
        this.shadow = shadow;
        this.width = font().width(text);
        this.height = font().lineHeight;
    }

    public Component getText() {
        return text;
    }

    @Override
    public void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.drawString(font(), text, x, y, ARGB.multiplyAlpha(color, alpha), shadow);
    }
}
