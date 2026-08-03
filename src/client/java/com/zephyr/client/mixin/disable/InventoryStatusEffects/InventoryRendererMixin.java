package com.zephyr.client.mixin.disable.InventoryStatusEffects;

import com.zephyr.client.module.disable.disableInventoryEffectRendering;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public abstract class InventoryRendererMixin {

    @Inject(method = "showsActiveEffects", at = @At("HEAD"), cancellable = true)
    private void zephyr$hideStatusEffects(CallbackInfoReturnable<Boolean> cir) {
        if (disableInventoryEffectRendering.INSTANCE.isEnabled()) {
            cir.setReturnValue(false);
        }
    }
}
