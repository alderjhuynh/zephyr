package com.zephyr.client.mixin;

import com.zephyr.client.module.HotbarRowSwap;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class HotbarRowSwapMouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void handleHotbarRowSwapScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (HotbarRowSwap.consumeScroll(vertical)) {
            ci.cancel();
        }
    }
}
