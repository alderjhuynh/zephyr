package com.zephyr.client.mixin.qol.FullBright;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.zephyr.client.module.qol.FullBright;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector4f;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Lightmap.class)
public abstract class LightmapMixin {
    @Shadow
    @Final
    private GpuTexture texture;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render$fullbright(LightmapRenderState renderState, CallbackInfo ci) {
        if (FullBright.INSTANCE.isEnabled()) {
            var profile = Profiler.get();
            profile.push("lightmap");

            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .clearColorTexture(texture, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
            profile.pop();
            ci.cancel();
        }
    }
}
