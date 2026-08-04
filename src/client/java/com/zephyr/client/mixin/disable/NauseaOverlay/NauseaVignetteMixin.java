package com.zephyr.client.mixin.disable.NauseaOverlay;

import com.zephyr.client.module.disable.disableNauseaOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class NauseaVignetteMixin {

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void zephyr$disableConfusionOverlay(GuiGraphicsExtractor graphics, float strength, CallbackInfo ci) {
        if (disableNauseaOverlay.INSTANCE.isEnabled()
                && Minecraft.getInstance().options.screenEffectScale().get() == 0.0) {
            ci.cancel();
        }
    }
}