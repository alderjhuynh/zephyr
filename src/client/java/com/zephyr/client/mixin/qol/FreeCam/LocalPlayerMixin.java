package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.zephyr.client.module.qol.FreeCam.MC;

/**
 * Mixin into {@link LocalPlayer} that reports the local player as the
 * controlled camera and redirects the player's Y-view rotation to the Zephyr
 * FreeCam module's {@link FreeCamera}.
 *
 * <p>{@code isControlledCamera} returning true keeps third-party mods (e.g.
 * Baritone) working during freecam, while {@code getViewYRot} makes
 * player-rotation consumers follow the camera. X-view rotation lives on
 * {@link net.minecraft.world.entity.Entity} in 26.3 (LocalPlayer no longer
 * overrides it), so it is handled in {@link EntityMixin} instead.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    /**
     * Makes the local player report itself as the controlled camera while
     * FreeCam is active; needed for Baritone compatibility.
     *
     * @param cir mixin callback used to override the result
     */
    @Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true)
    private void zephyr$isControlledCamera(CallbackInfoReturnable<Boolean> cir) {
        if (FreeCam.INSTANCE.isEnabled() && freecam$this() == MC.player) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Makes the player's Y-view rotation come from the FreeCamera when player
     * control and player-mode interactions are both disabled.
     *
     * <p>Note: {@code getViewXRot} is declared on {@link net.minecraft.world.entity.Entity}
     * in 26.3 and is not overridden by {@link LocalPlayer}, so hooking it here
     * fails validation ("could not find any targets"). It is hooked in
     * {@link EntityMixin} instead.
     *
     * @param partialTick the current partial tick
     * @param cir         mixin callback used to override the rotation
     */
    @Inject(method = "getViewYRot", at = @At("HEAD"), cancellable = true)
    private void zephyr$getViewYRot(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (FreeCam.isActive() && !FreeCam.isPlayerControlEnabled() && !FreeCam.allowInteractionsFromPlayer()) {
            cir.setReturnValue(FreeCam.getFreeCamera().getViewYRot(partialTick));
        }
    }

    /**
     * Casts the mixin {@code this} reference back to {@link LocalPlayer}.
     *
     * @return this object as a local player
     */
    @Unique
    private LocalPlayer freecam$this() {
        return (LocalPlayer) (Object) this;
    }
}
