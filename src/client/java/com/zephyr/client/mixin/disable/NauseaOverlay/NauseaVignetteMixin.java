package com.zephyr.client.mixin.disable.NauseaOverlay;

import com.zephyr.client.module.disable.disableNauseaOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link Hud} that backs the {@code disableNauseaOverlay} module.
 * It handles the confusion vignette half of the module's behaviour.
 */
@Mixin(Hud.class)
public abstract class NauseaVignetteMixin {

    /**
     * Cancels the HUD's {@code extractConfusionOverlay} at its head when the module is
     * enabled and the player's screen effect scale is zero, so the green nausea
     * vignette is never drawn.
     *
     * @param graphics the GUI graphics extractor for HUD rendering
     * @param strength the current confusion overlay strength
     * @param ci       the cancellable injection callback
     */
    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void zephyr$disableConfusionOverlay(GuiGraphicsExtractor graphics, float strength, CallbackInfo ci) {
        if (disableNauseaOverlay.INSTANCE.isEnabled()
                && Minecraft.getInstance().options.screenEffectScale().get() == 0.0) {
            ci.cancel();
        }
    }
}