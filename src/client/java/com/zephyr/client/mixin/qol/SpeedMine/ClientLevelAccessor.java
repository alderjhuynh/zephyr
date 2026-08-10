package com.zephyr.client.mixin.qol.SpeedMine;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Mixin invoker into {@link ClientLevel} exposing the private
 * {@code getBlockStatePredictionHandler} method.
 *
 * <p>Used by the SpeedMine module's DAMAGE mode to open a
 * {@link BlockStatePredictionHandler} prediction session when sending the early
 * STOP_DESTROY_BLOCK packet, keeping the client/server prediction sequences in
 * sync.
 */
@Mixin(ClientLevel.class)
public interface ClientLevelAccessor {

    /**
     * Invokes the private {@code ClientLevel.getBlockStatePredictionHandler}
     * method.
     *
     * @return the block state prediction handler for this client level
     */
    @Invoker("getBlockStatePredictionHandler")
    BlockStatePredictionHandler zephyr$getBlockStatePredictionHandler();
}