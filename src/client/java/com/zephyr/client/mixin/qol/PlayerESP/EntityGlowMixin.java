package com.zephyr.client.mixin.qol.PlayerESP;

import com.zephyr.client.module.qol.PlayerESP;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void glow(CallbackInfoReturnable<Boolean> cir) {
        if (!PlayerESP.INSTANCE.isEnabled()) return;
        if ((Object) this instanceof RemotePlayer) {
            cir.setReturnValue(true);
        }
    }
}
