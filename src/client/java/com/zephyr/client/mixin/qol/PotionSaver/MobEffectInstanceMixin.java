package com.zephyr.client.mixin.qol.PotionSaver;

import com.zephyr.client.module.qol.PotionSaver;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link MobEffectInstance} implementing the Zephyr PotionSaver
 * module's duration freeze.
 *
 * <p>Injects at the head of {@code MobEffectInstance.tickDownDuration}. When
 * {@link PotionSaver#shouldFreeze} reports that the effect's duration should be
 * preserved, the tick is cancelled so the remaining duration never decreases.
 */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {

    /**
     * Cancels the duration decrement for effects the PotionSaver module wants
     * to keep frozen.
     *
     * @param ci mixin callback used to cancel the duration tick
     */
    @Inject(method = "tickDownDuration", at = @At("HEAD"), cancellable = true)
    private void tick(CallbackInfo ci) {
        MobEffectInstance instance = (MobEffectInstance)(Object)this;

        if (PotionSaver.INSTANCE.shouldFreeze(instance.getEffect().value())) {
            ci.cancel();
        }
    }
}