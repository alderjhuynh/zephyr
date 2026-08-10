package com.zephyr.client.mixin.bettermovement;

import com.zephyr.client.configplusgui.secretsettings.bettermovement.BetterMovement;
import com.zephyr.client.configplusgui.secretsettings.bettermovement.WaveDash;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class WaveDashCrosshairMixin {

    private static final int TEX_SIZE = 15;

    private static final ResourceLocation WD_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "zephyr",
            "textures/gui/sprites/crosshair/crosshair-wavedash.png"
    );

    @Inject(method = "renderCrosshair", at = @At("TAIL"))
    private void zephyr$renderWaveDashCrosshair(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (!BetterMovement.enabled) return;
        if (!WaveDash.isBoosted()) return;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int x = (screenWidth - TEX_SIZE) / 2;
        int y = Math.round((screenHeight / 2.0f) - 8.0f);

        graphics.blit(
                WD_TEXTURE,
                x, y,
                0.0F, 0.0F,
                TEX_SIZE, TEX_SIZE,
                TEX_SIZE, TEX_SIZE
        );
    }
}
