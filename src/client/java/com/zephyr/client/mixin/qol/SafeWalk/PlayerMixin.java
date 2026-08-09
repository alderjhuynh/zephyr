package com.zephyr.client.mixin.qol.SafeWalk;

import com.zephyr.client.module.qol.SafeWalk;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
    private void zephyr$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        if (SafeWalk.INSTANCE.isEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
