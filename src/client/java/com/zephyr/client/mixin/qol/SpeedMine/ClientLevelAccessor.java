package com.zephyr.client.mixin.qol.SpeedMine;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientLevel.class)
public interface ClientLevelAccessor {

    @Invoker("getBlockStatePredictionHandler")
    BlockStatePredictionHandler zephyr$getBlockStatePredictionHandler();
}