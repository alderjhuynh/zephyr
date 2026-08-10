package com.zephyr.client.mixin.bettermovement;

import com.zephyr.client.configplusgui.secretsettings.bettermovement.BetterMovement;
import com.zephyr.client.configplusgui.secretsettings.bettermovement.Dash;
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
 * {@code extractRenderState} to draw the Better Movement dash cooldown icon near
 * the center of the screen while a dash cooldown is active. The icon is a frame
 * animation whose frame is chosen from the remaining cooldown, and it is only
 * rendered when Better Movement is enabled.
 */
@Mixin(Hud.class)
public class DashHudMixin {

    private static final int FRAME_COUNT = 10;
    private static final int TEX_SIZE = 9;

    private static final Identifier[] DASH_TEXTURES = new Identifier[FRAME_COUNT];

    static {
        for (int i = 0; i < FRAME_COUNT; i++) {
            DASH_TEXTURES[i] = Identifier.fromNamespaceAndPath(
                    "zephyr",
                    "dash/icon/dash" + (i + 1) + ".png"
            );
        }
    }

    /** Renders the dash cooldown icon at the end of the HUD's render-state extraction. */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void zephyr$renderDashCooldown(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (!BetterMovement.enabled) return;

        int cooldown = Dash.getCooldownTimer();
        if (cooldown == 0) return;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int x = (screenWidth - TEX_SIZE) / 2;
        int y = (screenHeight / 2) + 15;

        int frameIndex = computeFrameIndex(cooldown, Dash.getCooldownTicks());

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                DASH_TEXTURES[frameIndex],
                x, y,
                0.0F, 0.0F,
                TEX_SIZE, TEX_SIZE,
                TEX_SIZE, TEX_SIZE
        );
    }

    private static int computeFrameIndex(int cooldown, int maxCooldown) {
        if (cooldown <= 0) {
            return FRAME_COUNT - 1;
        }

        double progress = (double) cooldown / maxCooldown;

        int index = (int) Math.round(progress * (FRAME_COUNT - 1));
        return (FRAME_COUNT - 1) - clamp(index, 0, FRAME_COUNT - 1);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
