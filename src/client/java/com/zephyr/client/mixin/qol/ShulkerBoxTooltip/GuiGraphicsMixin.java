package com.zephyr.client.mixin.qol.ShulkerBoxTooltip;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zephyr.client.module.qol.shulkerboxtooltip.hook.GuiGraphicsExtensions;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin into {@link GuiGraphicsExtractor} that implements the
 * {@link GuiGraphicsExtensions} interface for the Zephyr ShulkerBoxTooltip
 * module.
 *
 * <p>Adds the module's private storage fields (tooltip top-left position and
 * mouse coordinates) as mixin-intrinsic fields on the graphics instance, exposes
 * the {@code deferredTooltip} field via accessors, and captures the tooltip's
 * top-left position during tooltip rendering so the content preview can be
 * positioned relative to the item tooltip.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsMixin implements GuiGraphicsExtensions {
    /** Stored X position of the top-left corner of the current tooltip. */
    @Unique
    private int zephyr$tooltipTopX = 0;
    /** Stored Y position of the top-left corner of the current tooltip. */
    @Unique
    private int zephyr$tooltipTopY = 0;
    /** Stored current mouse X position. */
    @Unique
    private int zephyr$mouseX = 0;
    /** Stored current mouse Y position. */
    @Unique
    private int zephyr$mouseY = 0;

    /**
     * Stores the tooltip's top-left X position.
     *
     * @param topX the X coordinate of the tooltip's top-left corner
     */
    @Intrinsic
    public void setTooltipTopXPosition(int topX) {
        this.zephyr$tooltipTopX = topX;
    }

    /**
     * Reads the stored tooltip top-left X position.
     *
     * @return the X coordinate of the tooltip's top-left corner
     */
    @Override
    @Intrinsic
    public int getTooltipTopXPosition() {
        return this.zephyr$tooltipTopX;
    }

    /**
     * Stores the tooltip's top-left Y position.
     *
     * @param topY the Y coordinate of the tooltip's top-left corner
     */
    @Intrinsic
    public void setTooltipTopYPosition(int topY) {
        this.zephyr$tooltipTopY = topY;
    }

    /**
     * Reads the stored tooltip top-left Y position.
     *
     * @return the Y coordinate of the tooltip's top-left corner
     */
    @Override
    @Intrinsic
    public int getTooltipTopYPosition() {
        return this.zephyr$tooltipTopY;
    }

    /**
     * Stores the current mouse X position.
     *
     * @param mouseX the mouse X position
     */
    @Override
    @Intrinsic
    public void setMouseX(int mouseX) {
        this.zephyr$mouseX = mouseX;
    }

    /**
     * Reads the stored mouse X position.
     *
     * @return the mouse X position
     */
    @Override
    @Intrinsic
    public int getMouseX() {
        return this.zephyr$mouseX;
    }

    /**
     * Stores the current mouse Y position.
     *
     * @param mouseY the mouse Y position
     */
    @Override
    @Intrinsic
    public void setMouseY(int mouseY) {
        this.zephyr$mouseY = mouseY;
    }

    /**
     * Reads the stored mouse Y position.
     *
     * @return the mouse Y position
     */
    @Override
    @Intrinsic
    public int getMouseY() {
        return this.zephyr$mouseY;
    }

    /**
     * Accessor exposing the deferred tooltip render action.
     *
     * @return the pending deferred tooltip, or null
     */
    @Accessor
    @Nullable
    public abstract Runnable getDeferredTooltip();

    /**
     * Accessor replacing the deferred tooltip render action.
     *
     * @param deferredTooltip the new deferred tooltip, or null to clear it
     */
    @Accessor
    public abstract void setDeferredTooltip(@Nullable Runnable deferredTooltip);

    /**
     * Captures the tooltip's top-left position so the preview can be positioned
     * relative to it. The positioner result is still returned unchanged.
     *
     * @param positioner  the tooltip positioner computing the placement
     * @param guiWidth    the GUI width used by the positioner
     * @param guiHeight   the GUI height used by the positioner
     * @param x           the mouse X coordinate
     * @param y           the mouse Y coordinate
     * @param totalWidth  the tooltip width
     * @param totalHeight the tooltip height
     * @param original    the wrapped positioner operation
     * @return the positioned tooltip coordinates
     */
    @WrapOperation(at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;"
            + "positionTooltip(IIIIII)Lorg/joml/Vector2ic;"), method = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V", require = 0)
    private Vector2ic zephyr$captureTooltipTopPosition(ClientTooltipPositioner positioner, int guiWidth, int guiHeight,
            int x, int y, int totalWidth, int totalHeight, Operation<Vector2ic> original) {
        Vector2ic result = original.call(positioner, guiWidth, guiHeight, x, y, totalWidth, totalHeight);
        var extendedGraphics = (GuiGraphicsExtensions) this;
        extendedGraphics.setTooltipTopXPosition(result.x());
        extendedGraphics.setTooltipTopYPosition(result.y());
        return result;
    }
}
