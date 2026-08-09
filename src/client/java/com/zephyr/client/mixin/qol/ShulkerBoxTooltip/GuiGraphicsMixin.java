package com.zephyr.client.mixin.qol.ShulkerBoxTooltip;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zephyr.client.module.qol.shulkerboxtooltip.hook.GuiGraphicsExtensions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin implements GuiGraphicsExtensions {
    @Unique
    private int zephyr$tooltipTopX = 0;
    @Unique
    private int zephyr$tooltipTopY = 0;
    @Unique
    private int zephyr$mouseX = 0;
    @Unique
    private int zephyr$mouseY = 0;

    @Intrinsic
    public void setTooltipTopXPosition(int topX) {
        this.zephyr$tooltipTopX = topX;
    }

    @Override
    @Intrinsic
    public int getTooltipTopXPosition() {
        return this.zephyr$tooltipTopX;
    }

    @Intrinsic
    public void setTooltipTopYPosition(int topY) {
        this.zephyr$tooltipTopY = topY;
    }

    @Override
    @Intrinsic
    public int getTooltipTopYPosition() {
        return this.zephyr$tooltipTopY;
    }

    @Override
    @Intrinsic
    public void setMouseX(int mouseX) {
        this.zephyr$mouseX = mouseX;
    }

    @Override
    @Intrinsic
    public int getMouseX() {
        return this.zephyr$mouseX;
    }

    @Override
    @Intrinsic
    public void setMouseY(int mouseY) {
        this.zephyr$mouseY = mouseY;
    }

    @Override
    @Intrinsic
    public int getMouseY() {
        return this.zephyr$mouseY;
    }

    @Accessor
    @Nullable
    public abstract Runnable getDeferredTooltip();

    @Accessor
    public abstract void setDeferredTooltip(@Nullable Runnable deferredTooltip);

    /**
     * Captures the tooltip's top-left position so the preview can be positioned relative to it.
     */
    @WrapOperation(at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;"
            + "positionTooltip(IIIIII)Lorg/joml/Vector2ic;"), method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V", require = 0)
    private Vector2ic zephyr$captureTooltipTopPosition(ClientTooltipPositioner positioner, int guiWidth, int guiHeight,
            int x, int y, int totalWidth, int totalHeight, Operation<Vector2ic> original) {
        Vector2ic result = original.call(positioner, guiWidth, guiHeight, x, y, totalWidth, totalHeight);
        var extendedGraphics = (GuiGraphicsExtensions) this;
        extendedGraphics.setTooltipTopXPosition(result.x());
        extendedGraphics.setTooltipTopYPosition(result.y());
        return result;
    }
}
