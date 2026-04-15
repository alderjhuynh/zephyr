package com.zephyr.client.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ClientWorld.class)
public interface ClientWorldAccessor {

    @Invoker("getPendingUpdateManager")
    PendingUpdateManager zephyr$getPendingUpdateManager();

    @Accessor("players")
    List<AbstractClientPlayerEntity> zephyr$getPlayers();
}
