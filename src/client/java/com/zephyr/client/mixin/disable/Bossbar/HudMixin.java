package com.zephyr.client.mixin.disable.Bossbar;

import com.zephyr.client.module.disable.disableBossbar;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudMixin {

    @Inject(method = "extractBossOverlay", at = @At("HEAD"), cancellable = true)
    private void zephyr$hideBossbar(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!disableBossbar.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
