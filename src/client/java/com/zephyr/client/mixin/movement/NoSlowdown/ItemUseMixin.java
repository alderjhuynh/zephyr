package com.zephyr.client.mixin.movement.NoSlowdown;

import com.zephyr.client.module.movement.NoSlowdown;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class ItemUseMixin {
    @Shadow
    @Final
    private Input input;

    private float zephyr$storedForwardImpulse;
    private float zephyr$storedLeftImpulse;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void zephyr$storeImpulses(CallbackInfo ci) {
        if (!NoSlowdown.INSTANCE.isEnabled()) return;
        this.zephyr$storedForwardImpulse = this.input.forwardImpulse;
        this.zephyr$storedLeftImpulse = this.input.leftImpulse;
    }

    // aiStep scales input.forwardImpulse/leftImpulse by 0.2 while using an item;
    // restore the unscaled values right after they are multiplied so movement keeps full speed.
    @Inject(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/Input;forwardImpulse:F", opcode = Opcodes.PUTFIELD))
    private void zephyr$restoreImpulses(CallbackInfo ci) {
        if (!NoSlowdown.INSTANCE.isEnabled()) return;
        this.input.forwardImpulse = this.zephyr$storedForwardImpulse;
        this.input.leftImpulse = this.zephyr$storedLeftImpulse;
    }
}
