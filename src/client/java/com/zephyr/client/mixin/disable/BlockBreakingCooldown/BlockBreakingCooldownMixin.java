package com.zephyr.client.mixin.disable.BlockBreakingCooldown;

import com.zephyr.client.module.disable.disableBlockBreakingCooldown;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class BlockBreakingCooldownMixin {

    @Shadow
    private int destroyDelay;

    @Inject(method = "tick", at = @At("HEAD"))
    private void removeBlockBreakingCooldown(CallbackInfo ci) {
        if (!disableBlockBreakingCooldown.INSTANCE.isEnabled()) {
            return;
        };
        this.destroyDelay = 0;
    }
}
