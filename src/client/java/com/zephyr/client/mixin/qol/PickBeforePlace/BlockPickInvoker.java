package com.zephyr.client.mixin.qol.PickBeforePlace;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface BlockPickInvoker {
    @Invoker("pickBlock")
    void invokePickBlock();
}
