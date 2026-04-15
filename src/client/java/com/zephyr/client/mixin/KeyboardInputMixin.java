package com.zephyr.client.mixin;

import com.zephyr.client.module.FreeCam;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void zephyr$freezePlayerInput(CallbackInfo ci) {
        if (!FreeCam.enabled) {
            return;
        }

        this.playerInput = new PlayerInput(false, false, false, false, false, false, false);
        this.movementVector = new Vec2f(0.0F, 0.0F);
    }
}
