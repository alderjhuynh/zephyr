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

@Mixin(Hud.class)
public class HudMixin {

    @Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"))
    private void zephyr$inventoryRenderer(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        InventoryRenderer.INSTANCE.render(graphics, Minecraft.getInstance().player);
    }
}
