package com.zephyr.client.mixin.qol.Sneak;

import com.zephyr.client.module.qol.Sneak;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link KeyboardInput} implementing the Zephyr Sneak module.
 *
 * <p>Injects at the tail of {@code KeyboardInput.tick} and rewrites the computed
 * key press state with the sneak flag forced to {@code true}, so the player
 * sneaks without holding the sneak key.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    /**
     * Forces the sneak bit in the player's computed input while the Sneak
     * module is enabled, preserving all other key states.
     *
     * @param ci mixin callback info (unused)
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void zephyr$forceSneak(CallbackInfo ci) {
        if (!Sneak.INSTANCE.isEnabled()) return;

        ClientInputAccessor accessor = (ClientInputAccessor) this;
        Input current = accessor.zephyr$getKeyPresses();

        accessor.zephyr$setKeyPresses(new Input(
                current.forward(), current.backward(),
                current.left(), current.right(),
                current.jump(), true, current.sprint()
        ));
    }
}