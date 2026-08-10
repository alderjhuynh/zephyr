package com.zephyr.client.mixin.disable.Bossbar;

import com.zephyr.client.module.disable.disableBossbar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public class HudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void zephyr$hideBossbar(GuiGraphics graphics, CallbackInfo ci) {
        if (!disableBossbar.INSTANCE.isEnabled()) return;
        ci.cancel();
    }
}
