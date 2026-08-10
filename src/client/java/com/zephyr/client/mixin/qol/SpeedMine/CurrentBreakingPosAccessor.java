package com.zephyr.client.mixin.qol.SpeedMine;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor into {@link MultiPlayerGameMode} exposing the private
 * {@code destroyBlockPos} and {@code destroyProgress} fields.
 *
 * <p>Used by the SpeedMine module's DAMAGE mode to poll the position and
 * progress of the block currently being broken on the client.
 */
@Mixin(MultiPlayerGameMode.class)
public interface CurrentBreakingPosAccessor {

    /**
     * Reads the {@code destroyBlockPos} field.
     *
     * @return the position of the block currently being broken, or null
     */
    @Accessor("destroyBlockPos")
    BlockPos getCurrentBreakingPos();

    /**
     * Reads the {@code destroyProgress} field.
     *
     * @return the client-side breaking progress of the current block
     */
    @Accessor("destroyProgress")
    float getCurrentBreakingProgress();
}