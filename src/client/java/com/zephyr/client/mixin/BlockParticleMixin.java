package com.zephyr.client.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.zephyr.client.disable.disableBlockBreakingParticles;


@Mixin(ClientWorld.class)
public class BlockParticleMixin {

    @Inject(
            method = "addBlockBreakParticles",
            at = @At("HEAD"),
            cancellable = true
    )
    private void disableBlockBreakingParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!disableBlockBreakingParticles.enabled) {return;}
        ci.cancel();
    }
}