package com.zephyr.client.mixin.qol.FreeCam;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor into {@link ClientInput} that exposes setters for the private
 * {@code keyPresses} and {@code moveVector} fields.
 *
 * <p>Used by the FreeCam {@link KeyboardInputMixin} to neutralize the local
 * player's input (writing {@link Input#EMPTY} and {@link Vec2#ZERO}) while the
 * camera is detached, so the player does not move.
 */
@Mixin(ClientInput.class)
public interface ClientInputAccessor {
    /**
     * Overwrites the {@link ClientInput#keyPresses} field.
     *
     * @param keyPresses the new key press state
     */
    @Accessor("keyPresses")
    void zephyr$setKeyPresses(Input keyPresses);

    /**
     * Overwrites the {@link ClientInput#moveVector} field.
     *
     * @param moveVector the new movement impulse vector
     */
    @Accessor("moveVector")
    void zephyr$setMoveVector(Vec2 moveVector);
}
