package com.zephyr.client.module.qol.jade.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Renders a 16x16 item icon at the left of the tooltip (like Jade's block / spawn
 * egg icons). Empty stacks render as nothing, which lets providers opt out of an
 * icon without special-casing.
 */
public class IconElement extends Element {
    public static final int SIZE = 16;

    private final ItemStack stack;

    public IconElement(ItemStack stack) {
        this.stack = stack;
        this.width = SIZE;
        this.height = SIZE;
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (!stack.isEmpty()) {
            graphics.item(stack, x, y);
        }
    }
}
