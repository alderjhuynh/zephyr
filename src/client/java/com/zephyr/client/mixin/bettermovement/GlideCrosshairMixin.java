package com.zephyr.client.mixin.bettermovement;

import com.zephyr.client.configplusgui.secretsettings.bettermovement.BetterMovement;
import com.zephyr.client.configplusgui.secretsettings.bettermovement.Glide;
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

@Mixin(Hud.class)
public class GlideCrosshairMixin {

    private static final int TEX_SIZE = 15;

    private static final Identifier GLIDE_TEXTURE = Identifier.fromNamespaceAndPath(
            "zephyr",
            "textures/gui/sprites/crosshair/crosshair-glide.png"
    );

    @Inject(method = "extractCrosshair", at = @At("TAIL"))
    private void zephyr$renderGlideCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!BetterMovement.enabled) return;
        if (!Glide.isGliding) return;

        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int x = (screenWidth - TEX_SIZE) / 2;
        int y = Math.round((screenHeight / 2.0f) - 8.0f);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                GLIDE_TEXTURE,
                x, y,
                0.0F, 0.0F,
                TEX_SIZE, TEX_SIZE,
                TEX_SIZE, TEX_SIZE
        );
    }
}
