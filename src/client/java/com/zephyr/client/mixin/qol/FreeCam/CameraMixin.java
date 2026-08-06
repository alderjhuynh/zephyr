package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void zephyr$freecam(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!FreeCam.INSTANCE.isEnabled()) return;

        FreeCam freecam = FreeCam.INSTANCE;
        if (freecam.getPos() == null) {
            freecam.initialize();
            if (freecam.getPos() == null) return;
        }

        freecam.onCameraUpdate(deltaTracker);
        this.setPosition(freecam.getPos());
        this.setRotation(freecam.getYaw(), freecam.getPitch());
        this.detached = freecam.shouldShowPlayer();
    }
}
