package com.zephyr.client.mixin.qol.Sneak;

import net.minecraft.client.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface ClientInputAccessor {
    @Accessor("shiftKeyDown")
    void zephyr$setShiftKeyDown(boolean shiftKeyDown);
}
