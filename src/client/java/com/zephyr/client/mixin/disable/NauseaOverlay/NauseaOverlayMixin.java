package com.zephyr.client.mixin.disable.NauseaOverlay;

import com.zephyr.client.module.disable.disableNauseaOverlay;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class NauseaOverlayMixin {

    @ModifyExpressionValue(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F")
    )
    private float zephyr$suppressNauseaWobble(float original) {
        if (disableNauseaOverlay.INSTANCE.isEnabled()
                && Minecraft.getInstance().options.screenEffectScale().get() == 0.0) {
            return 0.0f;
        }
        return original;
    }
}