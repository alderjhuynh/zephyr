package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import com.zephyr.client.module.qol.freecam.FreeCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link Camera} with two FreeCam-related tweaks: snapping the eye
 * height when switching between the player and the {@link FreeCamera} entity,
 * and hiding the water/lava/powdered-snow submersion fog.
 *
 * <p>Backs the Zephyr FreeCam module ("Show Submersion Fog" setting and the
 * instant eye-height transition when the camera entity is swapped in).
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    /** The entity the camera is currently tracking. */
    @Shadow
    private Entity entity;
    /** The camera's eye height from the previous frame. */
    @Shadow
    private float eyeHeightOld;
    /** The camera's current eye height. */
    @Shadow
    private float eyeHeight;

    /**
     * When the camera entity switches to or from the FreeCamera, snaps the eye
     * height immediately so there is no smooth transition.
     *
     * @param entity the new camera entity
     * @param ci     mixin callback info (unused)
     */
    @Inject(method = "setEntity", at = @At("HEAD"))
    private void zephyr$snapEyeHeight(Entity entity, CallbackInfo ci) {
        if (entity == null || this.entity == null) {
            return;
        }

        if (entity instanceof FreeCamera || this.entity instanceof FreeCamera) {
            this.eyeHeightOld = this.eyeHeight = entity.getEyeHeight();
        }
    }

    /**
     * Removes the submersion fog overlay when the FreeCam module is enabled
     * with "Show Submersion Fog" turned off.
     *
     * @param cir mixin callback used to force {@link FogType#NONE}
     */
    @Inject(method = "getFluidInCamera", at = @At("HEAD"), cancellable = true)
    private void zephyr$hideSubmersionFog(CallbackInfoReturnable<FogType> cir) {
        if (FreeCam.INSTANCE.isEnabled() && FreeCam.shouldHideSubmersionFog()) {
            cir.setReturnValue(FogType.NONE);
        }
    }
}
