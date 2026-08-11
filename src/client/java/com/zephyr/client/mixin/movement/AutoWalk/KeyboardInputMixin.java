package com.zephyr.client.mixin.movement.AutoWalk;

import com.zephyr.client.mixin.qol.GuiMove.ClientInputAccessor;
import com.zephyr.client.module.movement.AutoWalk;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link KeyboardInput} backing the {@code AutoWalk} module. Injects at the tail
 * of {@code KeyboardInput.tick}, after the vanilla input has been computed from the key
 * states, and forces the forward flag while preserving the other held inputs and the
 * forward movement impulse.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    /**
     * Forces the forward input when Auto Walk is enabled, keeping all other movement inputs.
     *
     * @param ci mixin callback info (unused)
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void zephyr$autoWalk(CallbackInfo ci) {
        if (!AutoWalk.INSTANCE.isEnabled()) return;

        ClientInputAccessor accessor = (ClientInputAccessor) this;
        Input current = accessor.zephyr$getKeyPresses();
        accessor.zephyr$setKeyPresses(new Input(
                true, false, current.left(), current.right(),
                current.jump(), current.shift(), current.sprint()));
        accessor.zephyr$setMoveVector(new Vec2(0.0F, 1.0F).normalized());
    }
}
