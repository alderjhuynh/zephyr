package com.zephyr.client.mixin.qol.Sneak;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientInput.class)
public interface ClientInputAccessor {
    @Accessor("keyPresses")
    Input zephyr$getKeyPresses();

    @Accessor("keyPresses")
    void zephyr$setKeyPresses(Input input);
}
