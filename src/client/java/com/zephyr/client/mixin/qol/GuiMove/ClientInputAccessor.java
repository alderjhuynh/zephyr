package com.zephyr.client.mixin.qol.GuiMove;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientInput.class)
public interface ClientInputAccessor {
    @Accessor("keyPresses")
    Input zephyr$getKeyPresses();

    @Accessor("keyPresses")
    void zephyr$setKeyPresses(Input input);

    @Accessor("moveVector")
    Vec2 zephyr$getMoveVector();

    @Accessor("moveVector")
    void zephyr$setMoveVector(Vec2 moveVector);
}