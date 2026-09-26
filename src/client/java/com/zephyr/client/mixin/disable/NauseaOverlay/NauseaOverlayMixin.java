package com.zephyr.client.mixin.disable.NauseaOverlay;

import com.zephyr.client.module.disable.disableNauseaOverlay;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin targeting {@link GameRenderer} that backs the {@code disableNauseaOverlay}
 * module. It handles the screen distortion (wobble) half of the module's behaviour.
 */
@Mixin(GameRenderer.class)
public abstract class NauseaOverlayMixin {

    /**
     * Replaces the wobble strength expression inside {@code GameRenderer#renderLevel}:
     * when the module is enabled and the player's screen effect scale is zero, the
     * distortion is forced to {@code 0.0f} so the nausea wobble never renders.
     *
     * @param original the original wobble strength value
     * @return {@code 0.0f} to suppress the wobble, otherwise {@code original}
     */
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