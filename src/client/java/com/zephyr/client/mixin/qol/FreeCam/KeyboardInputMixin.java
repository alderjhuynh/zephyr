package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import com.zephyr.client.module.qol.freecam.FreeCamera;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
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
        accessor.zephyr$setKeyPresses(Input.EMPTY);
        accessor.zephyr$setMoveVector(Vec2.ZERO);
        ci.cancel();
    }
}
