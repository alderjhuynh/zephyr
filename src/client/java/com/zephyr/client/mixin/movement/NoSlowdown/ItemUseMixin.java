package com.zephyr.client.mixin.movement.NoSlowdown;

import com.zephyr.client.module.movement.NoSlowdown;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting {@link net.minecraft.client.player.LocalPlayer}. Injects at
 * the return of {@code itemUseSpeedMultiplier()} and overrides the result with
 * 1.0 when the No Slowdown module is enabled, cancelling the movement slowdown
 * caused by using items (e.g. eating, drinking or blocking).
 */
@Mixin(LocalPlayer.class)
public abstract class ItemUseMixin {
    /** Removes the item-use speed reduction when No Slowdown is enabled. */
    @Inject(method = "itemUseSpeedMultiplier", at = @At("RETURN"), cancellable = true)
    private void zephyr$cancelItemUseSlowdown(CallbackInfoReturnable<Float> cir) {
        if (NoSlowdown.INSTANCE.isEnabled()) {
            cir.setReturnValue(1.0F);
        }
    }
}
