package com.zephyr.client.mixin.qol.InventoryRenderer;

import com.zephyr.client.module.qol.InventoryRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class HudMixin {

    @Inject(method = "renderHotbarAndDecorations", at = @At("RETURN"))
    private void zephyr$inventoryRenderer(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        InventoryRenderer.INSTANCE.render(graphics, Minecraft.getInstance().player);
    }
}
