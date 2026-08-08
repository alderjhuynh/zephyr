package com.zephyr.client.mixin.qol.MobESP;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void glow(CallbackInfoReturnable<Boolean> cir) {
        if (!MobESP.INSTANCE.isEnabled()) return;
        if (MobESP.INSTANCE.getColor(((Entity) (Object) this).getType()) != -1) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void glowColor(CallbackInfoReturnable<Integer> cir) {
        if (!MobESP.INSTANCE.isEnabled()) return;
        int color = MobESP.INSTANCE.getColor(((Entity) (Object) this).getType());
        if (color != -1) {
            cir.setReturnValue(color);
        }
    }
}
