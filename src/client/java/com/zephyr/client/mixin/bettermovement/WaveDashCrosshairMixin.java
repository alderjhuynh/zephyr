package com.zephyr.client.mixin.bettermovement;

import com.zephyr.client.configplusgui.secretsettings.bettermovement.BetterMovement;
import com.zephyr.client.configplusgui.secretsettings.bettermovement.WaveDash;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting {@link net.minecraft.client.gui.Hud}. Injects at the tail of
 * {@code extractCrosshair} to draw a custom wave-dash crosshair sprite while the
 * player is currently boosted by the Better Movement wave dash.
 */
@Mixin(Hud.class)
public class WaveDashCrosshairMixin {

    private static final int TEX_SIZE = 15;

    private static final Identifier WD_TEXTURE = Identifier.fromNamespaceAndPath(
            "zephyr",
            "textures/gui/sprites/crosshair/crosshair-wavedash.png"
    );

    /** Draws the wave-dash crosshair over the vanilla crosshair at the end of crosshair extraction. */
    @Inject(method = "extractCrosshair", at = @At("TAIL"))
    private void zephyr$renderWaveDashCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (!BetterMovement.enabled) return;
        if (!WaveDash.isBoosted()) return;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int x = (screenWidth - TEX_SIZE) / 2;
        int y = Math.round((screenHeight / 2.0f) - 8.0f);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                WD_TEXTURE,
                x, y,
                0.0F, 0.0F,
                TEX_SIZE, TEX_SIZE,
                TEX_SIZE, TEX_SIZE
        );
    }
}
