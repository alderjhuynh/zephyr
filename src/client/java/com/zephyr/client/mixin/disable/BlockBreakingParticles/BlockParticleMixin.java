package com.zephyr.client.mixin.disable.BlockBreakingParticles;

import com.zephyr.client.module.disable.disableBlockBreakingParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientLevel.class)
public class BlockParticleMixin {

    @Inject(
            method = "addDestroyBlockEffect",
            at = @At("HEAD"),
            cancellable = true
    )
    private void disableBlockBreakingParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!disableBlockBreakingParticles.INSTANCE.isEnabled()) {return;}
        ci.cancel();
    }
}
