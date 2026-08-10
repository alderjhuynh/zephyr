package com.zephyr.client.mixin.qol.Sneak;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor into {@link ClientInput} exposing getter and setter for the
 * private {@code keyPresses} field.
 *
 * <p>Used by the Sneak {@link KeyboardInputMixin} to read the current key press
 * state and rewrite it with sneak forced on.
 */
@Mixin(ClientInput.class)
public interface ClientInputAccessor {
    /**
     * Reads the {@link ClientInput#keyPresses} field.
     *
     * @return the current key press state
     */
    @Accessor("keyPresses")
    Input zephyr$getKeyPresses();

    /**
     * Overwrites the {@link ClientInput#keyPresses} field.
     *
     * @param input the new key press state
     */
    @Accessor("keyPresses")
    void zephyr$setKeyPresses(Input input);
}
