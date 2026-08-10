package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.zephyr.client.module.qol.FreeCam.MC;

/**
 * Mixin into {@link Entity} providing several FreeCam behaviors: routing mouse
 * look input to the {@link FreeCamera}, disabling entity-vs-FreeCamera pushing,
 * and freezing the local player in place when the "Freeze Player" setting is
 * enabled.
 *
 * <p>Backs the Zephyr FreeCam module's camera control and "Freeze Player"
 * feature.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    /**
     * Makes mouse input rotate the FreeCamera: when FreeCam is active, the
     * look deltas intended for the local player are redirected to the
     * FreeCamera entity instead.
     *
     * @param yawDelta   the horizontal look delta
     * @param pitchDelta the vertical look delta
     * @param ci         mixin callback used to cancel the player's own turn
     */
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void zephyr$freecamTurn(double yawDelta, double pitchDelta, CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled() && freecam$this() == MC.player && !FreeCam.isPlayerControlEnabled()) {
            FreeCam.getFreeCamera().turn(yawDelta, pitchDelta);
            ci.cancel();
        }
    }

    /**
     * Prevents the FreeCamera from pushing other entities and from being pushed
     * by them.
     *
     * @param entity the entity pushing this one
     * @param ci     mixin callback used to cancel the push
     */
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void zephyr$freecamPush(Entity entity, CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled() && (entity == FreeCam.getFreeCamera() || freecam$this() == FreeCam.getFreeCamera())) {
            ci.cancel();
        }
    }

    /**
     * Cancels velocity changes on the local player while FreeCam's
     * "Freeze Player" setting is active.
     *
     * @param ci mixin callback used to cancel the velocity change
     */
    @Inject(method = "setDeltaMovement(DDD)V", at = @At("HEAD"), cancellable = true)
    private void zephyr$freezeVelocity(CallbackInfo ci) {
        if (freecam$shouldFreeze()) {
            ci.cancel();
        }
    }

    /**
     * Cancels relative movement on the local player while FreeCam's
     * "Freeze Player" setting is active.
     *
     * @param ci mixin callback used to cancel the movement
     */
    @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
    private void zephyr$freezeMoveRelative(CallbackInfo ci) {
        if (freecam$shouldFreeze()) {
            ci.cancel();
        }
    }

    /**
     * Cancels positional changes on the local player while FreeCam's
     * "Freeze Player" setting is active.
     *
     * @param ci mixin callback used to cancel the position change
     */
    @Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true)
    private void zephyr$freezePosition(CallbackInfo ci) {
        if (freecam$shouldFreeze()) {
            ci.cancel();
        }
    }

    /**
     * Cancels raw position updates on the local player while FreeCam's
     * "Freeze Player" setting is active.
     *
     * @param ci mixin callback used to cancel the position update
     */
    @Inject(method = "setPosRaw", at = @At("HEAD"), cancellable = true)
    private void zephyr$freezePositionRaw(CallbackInfo ci) {
        if (freecam$shouldFreeze()) {
            ci.cancel();
        }
    }

    /**
     * Casts the mixin {@code this} reference back to {@link Entity}.
     *
     * @return this object as an entity
     */
    @Unique
    private Entity freecam$this() {
        return (Entity) (Object) this;
    }

    /**
     * Whether the local player should be frozen: FreeCam active, the entity is
     * the local player, "Freeze Player" is on, and player control is disabled.
     *
     * @return true if movement/position updates on this entity should be blocked
     */
    @Unique
    private boolean freecam$shouldFreeze() {
        return FreeCam.INSTANCE.isEnabled() && freecam$this() == MC.player
                && FreeCam.shouldFreezePlayer() && !FreeCam.isPlayerControlEnabled();
    }
}
