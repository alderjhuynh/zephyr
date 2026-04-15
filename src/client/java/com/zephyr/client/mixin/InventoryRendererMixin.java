package com.zephyr.client.mixin;

import com.zephyr.client.disable.disableInventoryEffectRendering;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public abstract class InventoryRendererMixin {

    @Inject(method = "showsStatusEffects", at = @At("HEAD"), cancellable = true)
    private void zephyr$hideStatusEffects(CallbackInfoReturnable<Boolean> cir) {
        if (disableInventoryEffectRendering.enabled) {
            cir.setReturnValue(false);
        }
    }
}
