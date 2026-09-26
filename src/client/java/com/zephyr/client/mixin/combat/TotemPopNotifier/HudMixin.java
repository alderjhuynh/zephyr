package com.zephyr.client.mixin.combat.TotemPopNotifier;

import com.zephyr.client.module.combat.TotemPopNotifier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link Hud} backing the {@code TotemPopNotifier} module's on-screen counter.
 * Injects at the return of {@code Hud.extractHotbarAndDecorations} so the per-player totem
 * pop totals are layered on top of the vanilla HUD.
 */
@Mixin(Hud.class)
public class HudMixin {

    /**
     * Draws the totem pop counter for the local player after the hotbar is rendered.
     *
     * @param graphics     the HUD graphics context
     * @param deltaTracker the HUD frame delta tracker (unused)
     * @param ci           mixin callback info (unused)
     */
    @Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"))
    private void zephyr$totemPopCounter(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        TotemPopNotifier.INSTANCE.render(graphics, Minecraft.getInstance().font, graphics.guiWidth());
    }
}
