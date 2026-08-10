package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.zephyr.client.module.qol.FreeCam.MC;

@Mixin(Entity.class)
public abstract class EntityMixin {
    // Makes mouse input rotate the FreeCamera.
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void zephyr$freecamTurn(double yawDelta, double pitchDelta, CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled() && freecam$this() == MC.player && !FreeCam.isPlayerControlEnabled()) {
            FreeCam.getFreeCamera().turn(yawDelta, pitchDelta);
            ci.cancel();
        }
    }

    // Prevents FreeCamera from pushing/getting pushed by entities.
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void zephyr$freecamPush(Entity entity, CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled() && (entity == FreeCam.getFreeCamera() || freecam$this() == FreeCam.getFreeCamera())) {
            ci.cancel();
        }
    }

    // Freezes the player's position if freezePlayer is enabled.
    @Inject(method = "setDeltaMovement(DDD)V", at = @At("HEAD"), cancellable = true)
    private void zephyr$freezeVelocity(CallbackInfo ci) {
        if (freecam$shouldFreeze()) {
            ci.cancel();
        }
    }

    // Freezes the player's position if freezePlayer is enabled.
    @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
    private void zephyr$freezeMoveRelative(CallbackInfo ci) {
        if (freecam$shouldFreeze()) {
            ci.cancel();
        }
    }

    // Freezes the player's position if freezePlayer is enabled.
    @Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true)
    private void zephyr$freezePosition(CallbackInfo ci) {
        if (freecam$shouldFreeze()) {
            ci.cancel();
        }
    }

    // Freezes the player's position if freezePlayer is enabled.
    @Inject(method = "setPosRaw", at = @At("HEAD"), cancellable = true)
    private void zephyr$freezePositionRaw(CallbackInfo ci) {
        if (freecam$shouldFreeze()) {
            ci.cancel();
        }
    }

    @Unique
    private Entity freecam$this() {
        return (Entity) (Object) this;
    }

    @Unique
    private boolean freecam$shouldFreeze() {
        return FreeCam.INSTANCE.isEnabled() && freecam$this() == MC.player
                && FreeCam.shouldFreezePlayer() && !FreeCam.isPlayerControlEnabled();
    }
}
