package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void zephyr$freecamLook(double yawDelta, double pitchDelta, CallbackInfo ci) {
        if (!((Object) this instanceof LocalPlayer)) return;
        if (!FreeCam.INSTANCE.isEnabled()) return;

        FreeCam.INSTANCE.onLook(yawDelta, pitchDelta);
        ci.cancel();
    }
}
