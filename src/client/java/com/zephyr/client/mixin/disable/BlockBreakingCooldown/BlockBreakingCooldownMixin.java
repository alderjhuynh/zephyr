package com.zephyr.client.mixin.disable.BlockBreakingCooldown;

import com.zephyr.client.module.disable.disableBlockBreakingCooldown;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link MultiPlayerGameMode} that backs the
 * {@code disableBlockBreakingCooldown} module.
 */
@Mixin(MultiPlayerGameMode.class)
public class BlockBreakingCooldownMixin {

    @Shadow
    private int destroyDelay;

    /**
     * Forces the block-breaking {@code destroyDelay} down to zero at the head of
     * each {@code tick}, removing the cooldown between consecutive block breaks
     * while the module is enabled.
     *
     * @param ci the non-cancellable injection callback
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void removeBlockBreakingCooldown(CallbackInfo ci) {
        if (!disableBlockBreakingCooldown.INSTANCE.isEnabled()) {
            return;
        };
        this.destroyDelay = 0;
    }
}
