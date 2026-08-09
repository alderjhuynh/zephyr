package com.zephyr.client.mixin.disable.NauseaOverlay;

import com.zephyr.client.module.disable.disableNauseaOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class NauseaVignetteMixin {

    @Inject(method = "renderConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void zephyr$disableConfusionOverlay(GuiGraphics graphics, float strength, CallbackInfo ci) {
        if (disableNauseaOverlay.INSTANCE.isEnabled()
                && Minecraft.getInstance().options.screenEffectScale().get() == 0.0) {
            ci.cancel();
        }
    }
}