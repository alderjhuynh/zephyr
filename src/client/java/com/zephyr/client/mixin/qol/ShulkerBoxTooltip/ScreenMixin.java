package com.zephyr.client.mixin.qol.ShulkerBoxTooltip;

import com.zephyr.client.module.qol.shulkerboxtooltip.hook.GuiGraphicsExtensions;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link Screen} supporting the Zephyr ShulkerBoxTooltip module's
 * mouse tracking.
 *
 * <p>Injects before {@code Screen.extractRenderState} during
 * {@code extractRenderStateWithTooltipAndSubtitles} to store the current mouse
 * position on the {@link GuiGraphicsExtractor} via
 * {@link GuiGraphicsExtensions}, so the content preview can be positioned at
 * the cursor.
 */
@Mixin(Screen.class)
public class ScreenMixin {

    /**
     * Makes the current mouse position available via extensions to the
     * GuiGraphicsExtractor instance.
     *
     * @param graphics the graphics instance being rendered to
     * @param mouseX   the current mouse X position
     * @param mouseY   the current mouse Y position
     * @param delta    the partial tick delta
     * @param ci       mixin callback info (unused)
     */
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"), method = "extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V")
    private void zephyr$captureMousePosition(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta,
            CallbackInfo ci) {
        GuiGraphicsExtensions extensions = (GuiGraphicsExtensions) graphics;
        extensions.setMouseY(mouseY);
        extensions.setMouseX(mouseX);
    }
}
