package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import com.zephyr.client.module.qol.freecam.FreeCamera;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void zephyr$freezePlayerInput(CallbackInfo ci) {
        if (!FreeCam.INSTANCE.isEnabled()) return;

        // The FreeCamera has its own KeyboardInput that must keep reading movement keys.
        FreeCamera camera = FreeCam.getFreeCamera();
        if (camera != null && (Object) this == camera.input) return;

        ClientInputAccessor accessor = (ClientInputAccessor) this;
        accessor.zephyr$setUp(false);
        accessor.zephyr$setDown(false);
        accessor.zephyr$setLeft(false);
        accessor.zephyr$setRight(false);
        accessor.zephyr$setJumping(false);
        accessor.zephyr$setShiftKeyDown(false);
        accessor.zephyr$setForwardImpulse(0.0F);
        accessor.zephyr$setLeftImpulse(0.0F);
        ci.cancel();
    }
}
