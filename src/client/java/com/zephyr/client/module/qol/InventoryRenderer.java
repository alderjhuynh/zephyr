package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Shows the player's whole inventory on the HUD, similar to {@link ArmorRenderer}
 * but for all 36 storage slots plus the optional armor and offhand slots.
 */
public final class InventoryRenderer extends Module {
    public static final InventoryRenderer INSTANCE = new InventoryRenderer();

    private static final int ICON_SIZE = 16;
    private static final int SLOT_GAP = 1;
    private static final int PADDING = 2;
    private static final int COLS = 9;
    private static final int ROWS = 4;
    private static final int SELECTED_SLOT_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND_COLOR = 0x66000000;

    private static final int[] GRID_ORDER = buildGridOrder();

    /** Where the inventory panel is anchored on the HUD. */
    public enum Placement {
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        MIDDLE_LEFT,
        MIDDLE_RIGHT
    }

    private final EnumSetting<Placement> placement =
            new EnumSetting<>("Placement", Placement.BOTTOM_RIGHT);
    private final BooleanSetting showHotbar = new BooleanSetting("Show Hotbar", true);
    private final BooleanSetting showArmor = new BooleanSetting("Show Armor", true);
    private final BooleanSetting showOffhand = new BooleanSetting("Show Offhand", true);
    private final BooleanSetting showBackground = new BooleanSetting("Background", true);
    private final BooleanSetting highlightSelected = new BooleanSetting("Highlight Selected", true);

    private InventoryRenderer() {
        super("InventoryRenderer", "Shows your inventory on the HUD", Category.QOL);
        addSetting(placement);
        addSetting(showHotbar);
        addSetting(showArmor);
        addSetting(showOffhand);
        addSetting(showBackground);
        addSetting(highlightSelected);
    }

    /**
     * Rendering order for the 36 non-equipment slots: the three main inventory
     * rows (slots 9-35) on top, the hotbar (slots 0-8) on the bottom row.
     */
    private static int[] buildGridOrder() {
        int[] order = new int[36];
        int index = 0;
        for (int slot = 9; slot < 36; slot++) {
            order[index++] = slot;
        }
        for (int slot = 0; slot < 9; slot++) {
            order[index++] = slot;
        }
        return order;
    }

    /**
     * Draws the inventory grid (and optionally the armor and offhand slots) at
     * the configured HUD placement for the given player.
     *
     * @param graphics the HUD graphics context
     * @param player   the player whose inventory is drawn
     */
    public void render(GuiGraphicsExtractor graphics, Player player) {
        if (!isEnabled() || player == null) return;

        int slotWidth = ICON_SIZE + SLOT_GAP;
        int gridWidth = COLS * ICON_SIZE + (COLS - 1) * SLOT_GAP;
        int rows = showHotbar.get() ? ROWS : ROWS - 1;
        int gridHeight = rows * ICON_SIZE + (rows - 1) * SLOT_GAP;

        boolean drawArmor = showArmor.get();
        boolean drawOffhand = showOffhand.get();
        int extraLeft = drawArmor ? slotWidth : 0;
        int extraRight = drawOffhand ? slotWidth : 0;

        int totalWidth = gridWidth + extraLeft + extraRight + PADDING * 2;
        int totalHeight = gridHeight + PADDING * 2;

        Placement currentPlacement = placement.get();
        int x = anchorX(graphics, totalWidth, currentPlacement);
        int y = anchorY(graphics, totalHeight, currentPlacement);

        if (showBackground.get()) {
            graphics.fill(x, y, x + totalWidth, y + totalHeight, BACKGROUND_COLOR);
        }

        int gridX = x + PADDING + extraLeft;
        int gridY = y + PADDING;

        List<ItemStack> stacks = player.getInventory().getNonEquipmentItems();
        int selectedSlot = player.getInventory().getSelectedSlot();
        Font font = Minecraft.getInstance().font;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < COLS; col++) {
                int slot = GRID_ORDER[row * COLS + col];
                drawSlot(graphics, font, player, stacks.get(slot),
                        gridX + col * slotWidth, gridY + row * slotWidth, slot == selectedSlot);
            }
        }

        if (drawArmor) {
            drawArmorColumn(graphics, font, player, x + PADDING, gridY, slotWidth);
        }

        if (drawOffhand) {
            int offhandX = gridX + gridWidth;
            int offhandY = gridY + (rows - 1) * slotWidth;
            drawSlot(graphics, font, player, player.getOffhandItem(), offhandX, offhandY, false);
        }
    }

    private static int anchorX(GuiGraphicsExtractor graphics, int totalWidth, Placement placement) {
        return switch (placement) {
            case BOTTOM_RIGHT, MIDDLE_RIGHT -> graphics.guiWidth() - totalWidth;
            case BOTTOM_LEFT, MIDDLE_LEFT -> 0;
        };
    }

    private static int anchorY(GuiGraphicsExtractor graphics, int totalHeight, Placement placement) {
        return switch (placement) {
            case BOTTOM_LEFT, BOTTOM_RIGHT -> graphics.guiHeight() - totalHeight;
            case MIDDLE_LEFT, MIDDLE_RIGHT -> (graphics.guiHeight() - totalHeight) / 2;
        };
    }

    private void drawArmorColumn(GuiGraphicsExtractor graphics, Font font, Player player,
                                 int x, int y, int slotWidth) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            drawSlot(graphics, font, player, player.getItemBySlot(slots[i]),
                    x, y + i * slotWidth, false);
        }
    }

    private void drawSlot(GuiGraphicsExtractor graphics, Font font, Player player,
                          ItemStack stack, int x, int y, boolean selected) {
        if (selected && highlightSelected.get()) {
            graphics.outline(x, y, ICON_SIZE, ICON_SIZE, SELECTED_SLOT_COLOR);
        }
        if (stack.isEmpty()) return;
        graphics.item(player, stack, x, y, 0);
        graphics.itemDecorations(font, stack, x, y);
    }
}
