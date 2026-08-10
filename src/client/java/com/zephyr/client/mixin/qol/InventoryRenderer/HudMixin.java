package com.zephyr.client.mixin.qol.InventoryRenderer;

import com.zephyr.client.module.qol.InventoryRenderer;
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
 * rendering to draw the Zephyr InventoryRenderer module's inventory panel.
 *
 * <p>Injects at the return of {@code Hud.extractHotbarAndDecorations} so the
 * inventory grid is layered on top of the vanilla HUD. The actual rendering is
 * delegated to {@link com.zephyr.client.module.qol.InventoryRenderer}.
 */
@Mixin(Hud.class)
public class HudMixin {

    /**
     * Invoked after the hotbar and its decorations are drawn; renders the
     * InventoryRenderer panel for the local player.
     *
     * @param graphics     the HUD graphics context
     * @param deltaTracker the HUD frame delta tracker (unused)
     * @param ci           mixin callback info (unused)
     */
    @Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"))
    private void zephyr$inventoryRenderer(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        InventoryRenderer.INSTANCE.render(graphics, Minecraft.getInstance().player);
    }
}
