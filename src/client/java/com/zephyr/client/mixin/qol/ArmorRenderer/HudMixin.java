package com.zephyr.client.mixin.qol.ArmorRenderer;

import com.zephyr.client.module.qol.ArmorRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link Hud} that hooks the end of the hotbar and decoration
 * rendering to draw the Zephyr ArmorRenderer module's armor columns.
 *
 * <p>Injects at the return of {@code Hud.extractHotbarAndDecorations} so the
 * equipped-armor + durability columns are layered on top of the vanilla HUD,
 * aligned with the hotbar. The actual rendering is delegated to
 * {@link com.zephyr.client.module.qol.ArmorRenderer}.
 */
@Mixin(Hud.class)
public class HudMixin {

    /**
     * Invoked after the hotbar and its decorations are drawn; renders the
     * ArmorRenderer columns beside the hotbar for the local player.
     *
     * @param graphics     the HUD graphics context
     * @param deltaTracker the HUD frame delta tracker (unused)
     * @param ci           mixin callback info (unused)
     */
    @Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"))
    private void zephyr$armorRenderer(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ArmorRenderer.INSTANCE.render(graphics, Minecraft.getInstance().player);
    }
}
