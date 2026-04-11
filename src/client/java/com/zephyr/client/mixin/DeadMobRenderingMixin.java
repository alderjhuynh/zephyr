package com.zephyr.client.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.zephyr.client.disable.disableDeadMobRendering;

@Mixin(LivingEntityRenderer.class)
public class DeadMobRenderingMixin<T extends LivingEntity> {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cancelDeadMobRendering(
            T entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (entity == null) return;
        if (!disableDeadMobRendering.enabled) return;
        if (!entity.isAlive()
                || entity.isRemoved()
                || entity.deathTime > 0) {
            ci.cancel();
        }
    }
}