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

    /**
     * Creates an icon element for the given item stack.
     *
     * @param stack the stack to render, or an empty stack to render nothing
     */
    public IconElement(ItemStack stack) {
        this.stack = stack;
        this.width = SIZE;
        this.height = SIZE;
    }

    /** @return the item stack this element renders */
    public ItemStack getStack() {
        return stack;
    }

    /**
     * Renders the item icon at the element's current position unless the stack
     * is empty.
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (!stack.isEmpty()) {
            graphics.item(stack, x, y);
        }
    }
}
