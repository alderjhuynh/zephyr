package com.zephyr.client.mixin.movement.NoSlowdown;

import com.zephyr.client.module.movement.NoSlowdown;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting {@link net.minecraft.world.entity.LivingEntity}. Injects at
 * the return of {@code getWaterSlowDown()} and overrides the result with 1.0 for
 * the local player when the No Slowdown module is enabled, cancelling the
 * movement slowdown from pushing through water.
 */
@Mixin(LivingEntity.class)
public abstract class WaterMixin {
    /** Removes the water push slowdown for the local player when No Slowdown is enabled. */
    @Inject(method = "getWaterSlowDown", at = @At("RETURN"), cancellable = true)
    private void zephyr$cancelWaterSlowdown(CallbackInfoReturnable<Float> cir) {
        if (NoSlowdown.INSTANCE.isEnabled() && (Object) this == Minecraft.getInstance().player) {
            cir.setReturnValue(1.0F);
        }
    }
}
