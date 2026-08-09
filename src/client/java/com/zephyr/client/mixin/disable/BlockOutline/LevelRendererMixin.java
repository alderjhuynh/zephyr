package com.zephyr.client.mixin.disable.BlockOutline;

import com.zephyr.client.module.disable.disableBlockOutline;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "setRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void zephyr$hideBlockOutline(boolean renderBlockOutline, CallbackInfo ci) {
        if (!disableBlockOutline.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
