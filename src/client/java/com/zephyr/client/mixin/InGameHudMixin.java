package com.zephyr.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    private static final int SLOT_WIDTH = 22;
    private static final int SLOT_HEIGHT = 22;
    private static final int ITEM_SIZE = 16;
    private static final int SLOT_GAP = 2;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void renderArmorDurability(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer textRenderer = client.textRenderer;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int baseX = screenWidth / 2 + 96;
        int y = screenHeight - 22;

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack stack = client.player.getEquippedStack(ARMOR_SLOTS[i]);
            if (stack.isEmpty() || !stack.isDamageable()) continue;

            int x = baseX + i * (SLOT_WIDTH + SLOT_GAP);
            int itemX = x + (SLOT_WIDTH - ITEM_SIZE) / 2;
            int itemY = y + (SLOT_HEIGHT - ITEM_SIZE) / 2;

            context.drawItem(stack, itemX, itemY);
            context.drawItemInSlot(textRenderer, stack, itemX, itemY);

            int damage = stack.getDamage();
            int maxDamage = stack.getMaxDamage();
            int remaining = maxDamage - damage;

            float percent = (float) remaining / maxDamage;

            int color;
            if (percent >= 1.0f) {
                color = 0xFFFFFFFF;
            } else if (percent >= 0.5f) {
                color = 0xFF55FF55;
            } else if (percent >= 0.25f) {
                color = 0xFFFFFF55;
            } else {
                color = 0xFFFF5555;
            }

            String text = String.valueOf(remaining);

            int textWidth = textRenderer.getWidth(text);
            int textY = itemY - ITEM_SIZE;

            context.drawText(textRenderer, text, itemX, textY, color, true);
        }
    }
}