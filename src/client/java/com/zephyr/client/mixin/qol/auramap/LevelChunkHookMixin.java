package com.zephyr.client.mixin.qol.auramap;

import com.zephyr.client.module.qol.auramap.world.ChunkDirtyTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Notifies the ported AuraMap's dirty tracker when a client chunk block changes
 * so the map re-captures edited chunks without a full rescan.
 */
@Mixin(LevelChunk.class)
public class LevelChunkHookMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void zephyr$auramap$onSetBlock(BlockPos pos, BlockState state, int flags,
                                           CallbackInfoReturnable<BlockState> cir) {
        LevelChunk self = (LevelChunk) (Object) this;

        if (self.getLevel() != null && self.getLevel().isClientSide()) {
            ChunkDirtyTracker.markDirty(self.getPos().x(), self.getPos().z());
        }
    }
}
