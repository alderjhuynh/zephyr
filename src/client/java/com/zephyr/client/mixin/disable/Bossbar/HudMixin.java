package com.zephyr.client.mixin.disable.Bossbar;

import com.zephyr.client.module.disable.disableBossbar;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link Hud} that backs the {@code disableBossbar} module.
 */
@Mixin(Hud.class)
public class HudMixin {

    /**
     * Cancels the HUD's {@code extractBossOverlay} at its head so that boss health
     * bars are never drawn while the module is enabled.
     *
     * @param extractor    the GUI graphics extractor for HUD rendering
     * @param deltaTracker the tick delta tracker
     * @param ci           the cancellable injection callback
     */
    @Inject(method = "extractBossOverlay", at = @At("HEAD"), cancellable = true)
    private void zephyr$hideBossbar(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!disableBossbar.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
