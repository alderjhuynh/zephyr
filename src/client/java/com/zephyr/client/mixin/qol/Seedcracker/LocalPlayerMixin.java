package com.zephyr.client.mixin.qol.Seedcracker;

import com.zephyr.client.module.qol.Seedcracker;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link LocalPlayer} that drives the Zephyr Seedcracker data
 * storage each tick.
 *
 * <p>Injects at the head of {@code LocalPlayer.tick} to advance the
 * {@link com.zephyr.client.module.qol.seedcracker.cracker.storage.DataStorage}
 * time machine / scheduled processing tied to the player's game tick.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    /**
     * Advances the Seedcracker's data storage tick while the player ticks.
     *
     * @param ci mixin callback info (unused)
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        Seedcracker.get().getDataStorage().tick();
    }
}
