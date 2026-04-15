package com.zephyr.client.mixin;

import com.zephyr.client.disable.disableNauseaOverlay;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class NauseaVignetteMixin {

    @Inject(
            method = "renderVignetteOverlay",
            at = @At("HEAD"),
            cancellable = true
    )
    private void disableVignetteOverlay(CallbackInfo ci) {
        if (!disableNauseaOverlay.enabled) {return;}
        ci.cancel();
    }
}