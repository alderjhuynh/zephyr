package com.zephyr.client.mixin.qol.GuiMove;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor into {@link ClientInput} exposing getters and setters for the
 * private {@code keyPresses} and {@code moveVector} fields.
 *
 * <p>Used by the GuiMove {@link KeyboardInputMixin} to read the current key
 * press state and overwrite both fields with keyboard-driven values while a GUI
 * is open.
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

    /**
     * Reads the {@link ClientInput#moveVector} field.
     *
     * @return the current movement impulse vector
     */
    @Accessor("moveVector")
    Vec2 zephyr$getMoveVector();

    /**
     * Overwrites the {@link ClientInput#moveVector} field.
     *
     * @param moveVector the new movement impulse vector
     */
    @Accessor("moveVector")
    void zephyr$setMoveVector(Vec2 moveVector);
}