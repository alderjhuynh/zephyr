package com.zephyr.client.mixin.qol.SpeedMine;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface CurrentBreakingPosAccessor {

    @Accessor("destroyBlockPos")
    BlockPos getCurrentBreakingPos();

    @Accessor("destroyProgress")
    float getCurrentBreakingProgress();
}