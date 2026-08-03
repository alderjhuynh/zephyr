package com.zephyr.client.mixin.disable.NauseaOverlay;

import com.zephyr.client.module.disable.disableNauseaOverlay;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class NauseaVignetteMixin {

    @Inject(
            method = "renderVignetteOverlay",
            at = @At("HEAD"),
            cancellable = true
    )
    private void disableVignetteOverlay(CallbackInfo ci) {
        if (!disableNauseaOverlay.INSTANCE.isEnabled()) {return;}
        ci.cancel();
    }
}
