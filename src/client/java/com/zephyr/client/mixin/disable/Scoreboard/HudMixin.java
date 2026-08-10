package com.zephyr.client.mixin.disable.Scoreboard;

import com.zephyr.client.module.disable.disableScoreboard;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link Hud} that backs the {@code disableScoreboard} module.
 */
@Mixin(Hud.class)
public class HudMixin {

    /**
     * Cancels the HUD's {@code extractScoreboardSidebar} at its head so that the
     * sidebar scoreboard is never drawn while the module is enabled.
     *
     * @param extractor    the GUI graphics extractor for HUD rendering
     * @param deltaTracker the tick delta tracker
     * @param ci           the cancellable injection callback
     */
    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void zephyr$hideScoreboard(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!disableScoreboard.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
