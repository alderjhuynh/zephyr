package com.zephyr.client.mixin.movement.AutoWalk;

import com.zephyr.client.mixin.qol.GuiMove.ClientInputAccessor;
import com.zephyr.client.module.movement.AutoWalk;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Backport of 26.3's AutoWalk KeyboardInput mixin to the 1.21.1 Input layout.
// 26.3 uses the record-style ClientInput(keyPresses, moveVector); 1.21.1 uses
// the classic Input fields (up/down/left/right/jumping/shiftKeyDown + impulses).
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void zephyr$autoWalk(boolean sprinting, float sneakPenalty, CallbackInfo ci) {
        if (!AutoWalk.INSTANCE.isEnabled()) return;

        ClientInputAccessor accessor = (ClientInputAccessor) this;
        accessor.zephyr$setUp(true);
        accessor.zephyr$setForwardImpulse(1.0F);
    }
}
