package com.zephyr.client.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.zephyr.client.disable.disableBlockBreakingCooldown;

@Mixin(ClientPlayerInteractionManager.class)
public class BlockBreakingCooldownMixin {

    @Shadow
    private int blockBreakingCooldown;

    @Inject(method = "tick", at = @At("HEAD"))
    private void removeBlockBreakingCooldown(CallbackInfo ci) {
        if (!disableBlockBreakingCooldown.enabled) {
            return;
        };
        this.blockBreakingCooldown = 0;
    }
}