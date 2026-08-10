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

/**
 * Mixin into {@link KeyboardInput} that neutralizes the local player's input
 * while the Zephyr FreeCam module is active.
 *
 * <p>Injects at the head of {@code KeyboardInput.tick}. Every input instance
 * other than the {@link FreeCamera}'s own input is zeroed out (empty key
 * presses and a zero movement vector) and the tick is cancelled, keeping the
 * player still while the camera flies.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    /**
     * Freezes the local player's keyboard input while FreeCam is active,
     * except for the FreeCamera's own input instance which must keep reading
     * movement keys.
     *
     * @param ci mixin callback used to cancel the input tick
     */
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
