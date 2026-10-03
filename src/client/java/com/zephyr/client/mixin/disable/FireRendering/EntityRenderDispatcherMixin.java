package com.zephyr.client.mixin.disable.FireRendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zephyr.client.module.disable.disableFireRendering;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Backport of 26.3's EntityRenderDispatcherMixin. 1.21.1 has no
// SubmitNodeCollector/submitFlame render path; the dispatcher renders the
// flame billboard in its own private renderFlame(), called from render()
// when entity.displayFireAnimation() is true.
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "renderFlame", at = @At("HEAD"), cancellable = true)
    private void zephyr$skipEntityFlame(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Entity entity,
            Quaternionf quaternion,
            CallbackInfo ci) {
        if (disableFireRendering.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}
