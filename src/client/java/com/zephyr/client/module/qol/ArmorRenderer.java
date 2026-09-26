package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the player's equipped armor as two columns on either side of the HUD
 * hotbar, each row showing the item icon plus its remaining durability.
 *
 * <p>The left column shows chestplate and helmet (plus the offhand item when
 * {@link #renderHandItems()} is enabled), and the right column shows boots and
 * leggings (plus the main hand item). Durability text is color-coded by the
 * remaining percentage: white above 75%, green above 50%, yellow above 25%,
 * and red below.
 */
public final class ArmorRenderer extends Module {
    public static final ArmorRenderer INSTANCE = new ArmorRenderer();

    private static final int ICON_SIZE = 16;
    private static final int FONT_HEIGHT = 9;
    private static final int ROW_GAP = 2;
    private static final int TEXT_GAP = 2;
    private static final int ROW_HEIGHT = ICON_SIZE + ROW_GAP;

    private final BooleanSetting renderHandItems = new BooleanSetting("Render Hand Items", false);

    private ArmorRenderer() {
        super("ArmorRenderer", "Shows equipped armor and held items on the HUD with their durability", Category.QOL);
        addSetting(renderHandItems);
    }

    /** Whether the main hand and offhand items are also rendered alongside the armor. */
    public boolean renderHandItems() {
        return renderHandItems.get();
    }

    /**
     * Renders the two armor columns next to the HUD hotbar for the given player.
     *
     * @param graphics the HUD graphics context
     * @param player   the player whose equipped items are drawn
     */
    public void render(GuiGraphicsExtractor graphics, Player player) {
        if (!isEnabled() || player == null) return;

        boolean showHands = renderHandItems.get();
        int screenCenter = graphics.guiWidth() / 2;
        int baseIconY = graphics.guiHeight() - 19 - 19 - 19;

        List<ItemStack> left = new ArrayList<>();
        left.add(player.getItemBySlot(EquipmentSlot.CHEST));
        left.add(player.getItemBySlot(EquipmentSlot.HEAD));
        if (showHands) left.add(player.getOffhandItem());

        List<ItemStack> right = new ArrayList<>();
        right.add(player.getItemBySlot(EquipmentSlot.FEET));
        right.add(player.getItemBySlot(EquipmentSlot.LEGS));
        if (showHands) right.add(player.getMainHandItem());

        Font font = Minecraft.getInstance().font;

        int leftIconX = screenCenter - 91 - ICON_SIZE - ROW_GAP;
        drawColumn(graphics, font, player, left, leftIconX, baseIconY, ColumnSide.LEFT);

        int rightIconX = screenCenter + 91 + ROW_GAP;
        drawColumn(graphics, font, player, right, rightIconX, baseIconY, ColumnSide.RIGHT);
    }

    private void drawColumn(GuiGraphicsExtractor graphics, Font font, Player player,
                            List<ItemStack> items, int iconX, int baseIconY, ColumnSide side) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            int iconY = baseIconY - i * ROW_HEIGHT;
            if (stack.isEmpty()) continue;
            drawItemRow(graphics, font, player, stack, iconX, iconY, side);
        }
    }

    private void drawItemRow(GuiGraphicsExtractor graphics, Font font, Player player,
                             ItemStack stack, int iconX, int iconY, ColumnSide side) {
        graphics.item(player, stack, iconX, iconY, 0);
        graphics.itemDecorations(font, stack, iconX, iconY);

        if (!stack.isDamageableItem()) return;

        String durabilityText = String.valueOf(stack.getMaxDamage() - stack.getDamageValue());
        int textY = iconY + (ICON_SIZE - FONT_HEIGHT) / 2;
        int textX = side == ColumnSide.LEFT
                ? iconX - TEXT_GAP - font.width(durabilityText)
                : iconX + ICON_SIZE + TEXT_GAP;
        graphics.text(font, durabilityText, textX, textY, durabilityColor(stack), true);
    }

    private static int durabilityColor(ItemStack stack) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 0xFFFFFFFF;
        }

        float ratio = (maxDamage - stack.getDamageValue()) / (float) maxDamage;
        if (ratio > 0.75F) {
            return 0xFFFFFFFF; // white
        }
        if (ratio > 0.5F) {
            return 0xFF00FF00; // green
        }
        if (ratio > 0.25F) {
            return 0xFFFFFF00; // yellow
        }
        return 0xFFFF0000; // red
    }

    private enum ColumnSide {
        LEFT,
        RIGHT
    }
}
