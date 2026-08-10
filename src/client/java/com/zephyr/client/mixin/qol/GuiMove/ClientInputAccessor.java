package com.zephyr.client.mixin.qol.GuiMove;

import net.minecraft.client.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface ClientInputAccessor {
    @Accessor("up")
    void zephyr$setUp(boolean up);

    @Accessor("down")
    void zephyr$setDown(boolean down);

    @Accessor("left")
    void zephyr$setLeft(boolean left);

    @Accessor("right")
    void zephyr$setRight(boolean right);

    @Accessor("jumping")
    void zephyr$setJumping(boolean jumping);

    @Accessor("shiftKeyDown")
    void zephyr$setShiftKeyDown(boolean shiftKeyDown);

    @Accessor("forwardImpulse")
    void zephyr$setForwardImpulse(float forwardImpulse);

    @Accessor("leftImpulse")
    void zephyr$setLeftImpulse(float leftImpulse);
}
