package com.zephyr.client.mixin.bettermovement;

import com.zephyr.client.configplusgui.secretsettings.bettermovement.BetterMovement;
import com.zephyr.client.configplusgui.secretsettings.bettermovement.DoubleJump;
import com.zephyr.client.configplusgui.secretsettings.bettermovement.Glide;
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
public class DoubleJumpElytraHudMixin {

    private static final int FRAME_COUNT = 13;
    private static final int TEX_SIZE = 9;

    private static final ResourceLocation[] ELY_TEXTURES = new ResourceLocation[FRAME_COUNT];

    static {
        for (int i = 0; i < FRAME_COUNT; i++) {
            ELY_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(
                    "zephyr",
                    "ely/icon/ely" + (i + 1) + ".png"
            );
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void zephyr$renderDoubleJumpElytraCooldown(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (!BetterMovement.enabled) return;

        if (!client.player.isFallFlying()) {
            if (!Glide.isGliding) {
                return;
            }
        }

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
                ELY_TEXTURES[frameIndex],
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
