package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link Minecraft} handling FreeCam interaction rules: blocking
 * attack/pick/block-breaking while interactions are disabled, making ray-cast
 * hits originate from the player when player-mode interaction is chosen, and
 * disabling FreeCam on disconnect.
 *
 * <p>Backs the Zephyr FreeCam module's "Allow Interactions" / "Interaction
 * Mode" settings.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /**
     * Prevents attacks while FreeCam is active and interactions are disabled.
     *
     * @param cir mixin callback used to cancel the attack
     */
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void zephyr$blockAttack(CallbackInfoReturnable<Boolean> cir) {
        if (zephyr$disableInteract()) {
            cir.cancel();
        }
    }

    /**
     * Prevents item pick while FreeCam is active and interactions are disabled.
     *
     * @param ci mixin callback used to cancel the pick
     */
    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void zephyr$blockPick(CallbackInfo ci) {
        if (zephyr$disableInteract()) {
            ci.cancel();
        }
    }

    /**
     * Makes mouse-click ray casts originate from the player rather than the
     * FreeCamera when player control is enabled or the interaction mode is set
     * to PLAYER.
     *
     * @param entity the entity currently used as the hit target source
     * @return the entity to use for the ray cast
     */
    @ModifyVariable(method = "pick(F)V", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/Minecraft;getCameraEntity()Lnet/minecraft/world/entity/Entity;"))
    private Entity zephyr$hitTargetSource(Entity entity) {
        if (FreeCam.INSTANCE.isEnabled() && (FreeCam.isPlayerControlEnabled() || FreeCam.allowInteractionsFromPlayer())) {
            return Minecraft.getInstance().player;
        }
        return entity;
    }

    /**
     * Prevents block breaking while FreeCam is active and interactions are
     * disabled.
     *
     * @param ci mixin callback used to cancel the continuing attack
     */
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void zephyr$blockContinueAttack(CallbackInfo ci) {
        if (zephyr$disableInteract()) {
            ci.cancel();
        }
    }

    /**
     * Disables FreeCam when the player disconnects.
     *
     * @param ci mixin callback info (unused)
     */
    @Inject(method = "disconnect*", at = @At(value = "HEAD"))
    private void zephyr$freecamDisconnect(CallbackInfo ci) {
        FreeCam.onDisconnect();
    }

    /**
     * Whether FreeCam is active, player control is off, and the "Allow
     * Interactions" setting is disabled.
     *
     * @return true if interactions should be suppressed
     */
    @Unique
    private static boolean zephyr$disableInteract() {
        return FreeCam.INSTANCE.isEnabled() && !FreeCam.isPlayerControlEnabled() && FreeCam.shouldPreventInteractions();
    }
}
