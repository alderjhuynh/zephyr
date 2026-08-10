package com.zephyr.client.module.qol.shulkerboxtooltip.hook;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Provides access to the {@link GuiGraphics} methods added by the module's
 * {@code GuiGraphicsMixin}.
 */
public interface GuiGraphicsExtensions {
    void setMouseX(int mouseX);

    int getMouseX();

    void setMouseY(int mouseY);

    int getMouseY();
}
