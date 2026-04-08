package com.zephyr.client.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityInvoker {
    @Invoker("sendMovementPackets")
    void zephyr$sendMovementPackets();
}
