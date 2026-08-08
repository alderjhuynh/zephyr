package com.zephyr.client.mixin.qol.FreeCam;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.zephyr.client.module.qol.FreeCam.MC;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Unique
    private float zephyr$tickDelta;

    @Redirect(
            method = "submitHandsWithItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getViewXRot(F)F"
            )
    )
    private float zephyr$redirectGetViewXRot(LocalPlayer player, float partialTick) {
        return FreeCam.isActive() ? FreeCam.getFreeCamera().getViewXRot(partialTick) : player.getViewXRot(partialTick);
    }

    @Redirect(
            method = "submitHandsWithItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getViewYRot(F)F"
            )
    )
    private float zephyr$redirectGetViewYRot(LocalPlayer player, float partialTick) {
        return FreeCam.isActive() ? FreeCam.getFreeCamera().getViewYRot(partialTick) : player.getViewYRot(partialTick);
    }

    // Makes arm movement depend upon FreeCamera movement rather than player movement.
    @Redirect(
            method = "submitHandsWithItems",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;xBob:F",
                    opcode = Opcodes.GETFIELD)
    )
    private float zephyr$redirectGetXBob(LocalPlayer player) {
        return FreeCam.isActive() ? FreeCam.getFreeCamera().xBob : player.xBob;
    }

    // Makes arm movement depend upon FreeCamera movement rather than player movement.
    @Redirect(
            method = "submitHandsWithItems",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;xBobO:F",
                    opcode = Opcodes.GETFIELD)
    )
    private float zephyr$redirectGetXBobO(LocalPlayer player) {
        return FreeCam.isActive() ? FreeCam.getFreeCamera().xBobO : player.xBobO;
    }

    // Makes arm movement depend upon FreeCamera movement rather than player movement.
    @Redirect(
            method = "submitHandsWithItems",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;yBob:F",
                    opcode = Opcodes.GETFIELD)
    )
    private float zephyr$redirectGetYBob(LocalPlayer player) {
        return FreeCam.isActive() ? FreeCam.getFreeCamera().yBob : player.yBob;
    }

    // Makes arm movement depend upon FreeCamera movement rather than player movement.
    @Redirect(
            method = "submitHandsWithItems",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;yBobO:F",
                    opcode = Opcodes.GETFIELD)
    )
    private float zephyr$redirectGetYBobO(LocalPlayer player) {
        return FreeCam.isActive() ? FreeCam.getFreeCamera().yBobO : player.yBobO;
    }

    @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
    private void zephyr$storeTickDelta(float partialTick, PoseStack poseStack,
                                       SubmitNodeCollector nodeCollector,
                                       LocalPlayer player,
                                       int packedLight,
                                       CallbackInfo ci) {
        this.zephyr$tickDelta = partialTick;
    }

    // Makes arm shading depend upon FreeCamera position rather than player position.
    @ModifyVariable(method = "submitHandsWithItems", at = @At("HEAD"), argsOnly = true)
    private int zephyr$redirectLightCoords(int lightCoords) {
        if (FreeCam.isActive()) {
            return MC.getEntityRenderDispatcher().getPackedLightCoords(FreeCam.getFreeCamera(), zephyr$tickDelta);
        }
        return lightCoords;
    }
}
