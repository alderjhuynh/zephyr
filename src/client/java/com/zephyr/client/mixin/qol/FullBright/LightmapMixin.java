package com.zephyr.client.mixin.qol.FullBright;

import com.mojang.blaze3d.platform.NativeImage;
import com.zephyr.client.module.qol.FreeCam;
import com.zephyr.client.module.qol.FullBright;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(LightTexture.class)
public abstract class LightmapMixin {
    @Shadow
    @Final
    private NativeImage lightPixels;

    @Shadow
    @Final
    private DynamicTexture lightTexture;

    @Shadow
    private boolean updateLightTexture;

    @Inject(method = "updateLightTexture", at = @At("HEAD"), cancellable = true)
    private void zephyr$fullbright(float partialTicks, CallbackInfo ci) {
        boolean fullBright = FullBright.INSTANCE.isEnabled()
                || (FreeCam.INSTANCE.isEnabled() && FreeCam.isFullBrightEnabled());
        if (!fullBright) return;

        Arrays.fill(this.lightPixels.getPixelsRGBA(), 0xFFFFFFFF);
        this.lightTexture.upload();
        this.updateLightTexture = false;
        ci.cancel();
    }
}
