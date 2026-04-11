package com.zephyr.client.mixin;

import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.DrawContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.zephyr.client.disable.disableInventoryEffectRendering;

@Mixin(AbstractInventoryScreen.class)
public abstract class InventoryRendererMixin {

    @Inject(method = "drawStatusEffects", at = @At("HEAD"), cancellable = true)
    private void removeStatusEffects(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (!disableInventoryEffectRendering.enabled) return;
        ci.cancel();
    }
}
