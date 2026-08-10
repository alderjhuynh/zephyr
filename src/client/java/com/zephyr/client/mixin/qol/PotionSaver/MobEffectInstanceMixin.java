package com.zephyr.client.mixin.qol.PotionSaver;

import com.zephyr.client.module.qol.PotionSaver;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {

    @Inject(method = "tickDownDuration", at = @At("HEAD"), cancellable = true)
    private void tick(CallbackInfoReturnable<Integer> cir) {
        MobEffectInstance instance = (MobEffectInstance)(Object)this;

        if (PotionSaver.INSTANCE.shouldFreeze(instance.getEffect().value())) {
            cir.setReturnValue(instance.getDuration());
        }
    }
}