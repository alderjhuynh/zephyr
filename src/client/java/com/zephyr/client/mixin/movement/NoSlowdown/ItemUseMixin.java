package com.zephyr.client.mixin.movement.NoSlowdown;

import com.zephyr.client.module.movement.NoSlowdown;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class ItemUseMixin {
    @Inject(method = "itemUseSpeedMultiplier", at = @At("RETURN"), cancellable = true)
    private void zephyr$cancelItemUseSlowdown(CallbackInfoReturnable<Float> cir) {
        if (NoSlowdown.INSTANCE.isEnabled()) {
            cir.setReturnValue(1.0F);
        }
    }
}
