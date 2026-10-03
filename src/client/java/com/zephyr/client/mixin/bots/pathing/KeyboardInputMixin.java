package com.zephyr.client.mixin.bots.pathing;

import com.zephyr.client.mixin.qol.GuiMove.ClientInputAccessor;
import com.zephyr.client.module.bots.pathing.Pathing;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Backport of 26.3's pathing KeyboardInput mixin to the 1.21.1 Input layout
// (classic up/down/left/right/jumping/shiftKeyDown + impulses).
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void zephyr$pathing(boolean sprinting, float sneakPenalty, CallbackInfo ci) {
        Pathing module = Pathing.INSTANCE;
        if (!module.isEnabled() || !module.isActive()) return;

        ClientInputAccessor accessor = (ClientInputAccessor) this;

        // Hold still instead of walking off an edge that cannot be bridged yet.
        if (module.wantsPause()) {
            accessor.zephyr$setUp(false);
            accessor.zephyr$setDown(false);
            accessor.zephyr$setJumping(false);
            accessor.zephyr$setForwardImpulse(0.0F);
            accessor.zephyr$setLeftImpulse(0.0F);
            return;
        }

        accessor.zephyr$setUp(true);
        accessor.zephyr$setJumping(module.wantsJump());
        accessor.zephyr$setForwardImpulse(1.0F);
    }
}
