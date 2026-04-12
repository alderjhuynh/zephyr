package com.zephyr.client.mixin;

import com.zephyr.client.module.FreeCam;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void zephyr$freezePlayerInput(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (!FreeCam.enabled) {
            return;
        }

        this.pressingForward = false;
        this.pressingBack = false;
        this.pressingLeft = false;
        this.pressingRight = false;
        this.movementForward = 0.0F;
        this.movementSideways = 0.0F;
        this.jumping = false;
        this.sneaking = false;
    }
}
