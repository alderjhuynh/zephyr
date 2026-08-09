package com.zephyr.client.mixin.disable.Scoreboard;

import com.zephyr.client.module.disable.disableScoreboard;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class HudMixin {

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void zephyr$hideScoreboard(GuiGraphics extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!disableScoreboard.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
