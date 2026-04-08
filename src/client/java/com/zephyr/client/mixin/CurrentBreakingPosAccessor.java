package com.zephyr.client.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerInteractionManager.class)
public interface CurrentBreakingPosAccessor {

    @Accessor("currentBreakingPos")
    BlockPos getCurrentBreakingPos();

    @Accessor("currentBreakingProgress")
    float getCurrentBreakingProgress();
}
