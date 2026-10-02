package com.zephyr.client.mixin.disable.FireRendering;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zephyr.client.module.disable.disableFireRendering;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Backs the {@link disableFireRendering} module (burning entities).
 *
 * <p>Skips the {@code SubmitNodeCollector#submitFlame} call inside
 * {@code EntityRenderDispatcher#submit} while enabled, so entities with
 * {@code displayFireAnimation} render without the flame billboard. The
 * entity itself still renders normally.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @WrapOperation(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitFlame(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lorg/joml/Quaternionf;)V"
            )
    )
    private void zephyr$skipEntityFlame(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            EntityRenderState state,
            Quaternionf orientation,
            Operation<Void> original) {
        if (disableFireRendering.INSTANCE.isEnabled()) {
            return;
        }
        original.call(collector, poseStack, state, orientation);
    }
}
