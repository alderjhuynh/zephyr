package com.zephyr.client.mixin.qol.ShulkerBoxTooltip;

import com.zephyr.client.module.qol.shulkerboxtooltip.hook.GuiGraphicsExtensions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    /**
     * Makes the current mouse position available via extensions to the GuiGraphics instance.
     */
    @Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V")
    private void zephyr$captureMousePosition(GuiGraphics graphics, int mouseX, int mouseY, float delta,
            CallbackInfo ci) {
        GuiGraphicsExtensions extensions = (GuiGraphicsExtensions) graphics;
        extensions.setMouseY(mouseY);
        extensions.setMouseX(mouseX);
    }
}
