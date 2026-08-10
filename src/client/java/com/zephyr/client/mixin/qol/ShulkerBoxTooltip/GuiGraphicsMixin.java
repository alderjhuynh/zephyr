package com.zephyr.client.mixin.qol.ShulkerBoxTooltip;

import com.zephyr.client.module.qol.shulkerboxtooltip.hook.GuiGraphicsExtensions;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin implements GuiGraphicsExtensions {
    @Unique
    private int zephyr$mouseX = 0;
    @Unique
    private int zephyr$mouseY = 0;

    @Override
    public void setMouseX(int mouseX) {
        this.zephyr$mouseX = mouseX;
    }

    @Override
    public int getMouseX() {
        return this.zephyr$mouseX;
    }

    @Override
    public void setMouseY(int mouseY) {
        this.zephyr$mouseY = mouseY;
    }

    @Override
    public int getMouseY() {
        return this.zephyr$mouseY;
    }
}
