package com.zephyr.client.mixin.bettermovement;

import com.zephyr.client.configplusgui.secretsettings.bettermovement.BetterMovement;
import com.zephyr.client.configplusgui.secretsettings.bettermovement.DoubleJump;
import com.zephyr.client.configplusgui.secretsettings.bettermovement.Glide;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class DoubleJumpHudMixin {

    private static final int FRAME_COUNT = 13;
    private static final int TEX_SIZE = 9;

    private static final Identifier[] DJ_TEXTURES = new Identifier[FRAME_COUNT];

    static {
        for (int i = 0; i < FRAME_COUNT; i++) {
            DJ_TEXTURES[i] = Identifier.fromNamespaceAndPath(
                    "zephyr",
                    "dj/icon/dj" + (i + 1) + ".png"
            );
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void zephyr$renderDoubleJumpCooldown(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (!BetterMovement.enabled) return;

        if (client.player.isFallFlying() || Glide.isGliding) return;

        if (DoubleJump.getTicksAvailable() == DoubleJump.getCooldownTicks()) {
            return;
        }

        int maxTicks = DoubleJump.getCooldownTicks();

        float ticks = DoubleJump.getCooldownTimer() + deltaTracker.getGameTimeDeltaPartialTick(false);

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int x = (screenWidth - TEX_SIZE) / 2;
        int y = (screenHeight / 2) + 25;

        int frameIndex = computeFrameIndex(ticks, maxTicks);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                DJ_TEXTURES[frameIndex],
                x, y,
                0.0F, 0.0F,
                TEX_SIZE, TEX_SIZE,
                TEX_SIZE, TEX_SIZE
        );
    }

    private static int computeFrameIndex(float ticks, int maxTicks) {
        if (ticks <= 0) return FRAME_COUNT - 1;
        float t = ticks / (float) maxTicks;
        int index = (int) ((1.0f - t) * (FRAME_COUNT - 1));
        return clamp(index, 0, FRAME_COUNT - 1);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
