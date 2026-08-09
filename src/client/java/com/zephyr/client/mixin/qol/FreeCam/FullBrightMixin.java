package com.zephyr.client.mixin.qol.FreeCam;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public abstract class FullBrightMixin {
    @Shadow
    @Final
    private GpuTexture texture;

    @Inject(method = "updateLightTexture", at = @At("HEAD"), cancellable = true)
    private void zephyr$freecamFullbright(float partialTick, CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled() && FreeCam.isFullBrightEnabled()) {
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .clearColorTexture(texture, 0xFFFFFFFF);
            ci.cancel();
        }
    }
}
