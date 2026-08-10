package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    // Hide hand in freecam if showHand is disabled.
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void zephyr$hideHand(CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled() && FreeCam.shouldHideHand()) {
            ci.cancel();
        }
    }

    // Disables block outlines when allowInteract is disabled.
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void zephyr$blockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (FreeCam.INSTANCE.isEnabled() && !FreeCam.isPlayerControlEnabled() && FreeCam.shouldPreventInteractions()) {
            cir.setReturnValue(false);
        }
    }

    // Makes mouse clicks come from the player rather than the freecam entity when player control is enabled or if interaction mode is set to player.
    @ModifyVariable(method = "pick", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/Minecraft;getCameraEntity()Lnet/minecraft/world/entity/Entity;"))
    private Entity zephyr$hitTargetSource(Entity entity) {
        if (FreeCam.INSTANCE.isEnabled() && (FreeCam.isPlayerControlEnabled() || FreeCam.allowInteractionsFromPlayer())) {
            return Minecraft.getInstance().player;
        }
        return entity;
    }
}
