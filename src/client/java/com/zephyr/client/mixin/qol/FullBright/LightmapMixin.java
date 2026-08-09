package com.zephyr.client.mixin.qol.FullBright;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.zephyr.client.module.qol.FullBright;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public abstract class LightmapMixin {
    @Shadow
    @Final
    private GpuTexture texture;

    @Inject(method = "updateLightTexture", at = @At("HEAD"), cancellable = true)
    private void render$fullbright(float partialTick, CallbackInfo ci) {
        if (FullBright.INSTANCE.isEnabled()) {
            var profile = Profiler.get();
            profile.push("lightmap");

            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .clearColorTexture(texture, 0xFFFFFFFF);
            profile.pop();
            ci.cancel();
        }
    }
}
