package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.zephyr.client.module.qol.FreeCam.MC;

@Mixin(Gui.class)
public abstract class HudMixin {
    // Makes HUD correspond to the player rather than the FreeCamera.
    @Inject(method = "getCameraPlayer", at = @At("HEAD"), cancellable = true)
    private void zephyr$cameraPlayer(CallbackInfoReturnable<Player> cir) {
        if (FreeCam.INSTANCE.isEnabled()) {
            cir.setReturnValue(MC.player);
        }
    }

    // Don't render equipped-item overlays while Freecam is active
    @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void zephyr$textureOverlay(
            GuiGraphics graphics,
            ResourceLocation texture,
            float alpha,
            CallbackInfo ci) {
        if (FreeCam.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}
