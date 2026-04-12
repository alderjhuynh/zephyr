package com.zephyr.client.module;

import com.zephyr.client.keybind.ZephyrKeybindManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public final class HotbarRowSwap {
    private static final int HOTBAR_SIZE = 9;
    private static final int INVENTORY_ROWS = 3;
    private static final int FIRST_MAIN_INVENTORY_SLOT = 9;
    private static final int DEFAULT_ROW = 2;

    private static final int PANEL_PADDING = 6;
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_SPACING = 18;
    private static final int ROW_WIDTH = (HOTBAR_SIZE * SLOT_SPACING) - (SLOT_SPACING - SLOT_SIZE);
    private static final int HEADER_HEIGHT = 10;
    private static final int FOOTER_HEIGHT = 10;
    private static final int PANEL_WIDTH = ROW_WIDTH + (PANEL_PADDING * 2);
    private static final int PANEL_HEIGHT = HEADER_HEIGHT + FOOTER_HEIGHT + (INVENTORY_ROWS + 1) * SLOT_SPACING + (PANEL_PADDING * 2);
    private static final float HUD_SCALE = 0.75F;

    public static boolean enabled = false;

    private static boolean keyHeldLastTick;
    private static boolean selectionActive;
    private static int selectedRow = DEFAULT_ROW;

    private HotbarRowSwap() {
    }

    public static void tick(MinecraftClient client) {
        if (!enabled || client == null || client.player == null || client.interactionManager == null || client.currentScreen != null) {
            clearSelection();
            return;
        }

        boolean keyHeld = ZephyrKeybindManager.isActionHeld(client, ZephyrKeybindManager.Action.HOTBAR_ROW_SWAP);

        if (keyHeld && !keyHeldLastTick) {
            selectionActive = true;
        } else if (!keyHeld && keyHeldLastTick) {
            swapSelectedRow(client);
            selectionActive = false;
        }

        keyHeldLastTick = keyHeld;
    }

    public static boolean consumeScroll(double verticalAmount) {
        if (!selectionActive || Math.abs(verticalAmount) < 1.0E-3D) {
            return false;
        }

        if (verticalAmount > 0.0D) {
            selectedRow = Math.max(0, selectedRow - 1);
        } else {
            selectedRow = Math.min(INVENTORY_ROWS - 1, selectedRow + 1);
        }

        return true;
    }

    public static void renderOverlay(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!selectionActive || client.player == null || client.currentScreen != null) {
            return;
        }

        int scaledWidth = Math.round(PANEL_WIDTH * HUD_SCALE);
        int scaledHeight = Math.round(PANEL_HEIGHT * HUD_SCALE);
        int x = context.getScaledWindowWidth() - scaledWidth - 8;
        int y = context.getScaledWindowHeight() - scaledHeight - 28;

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0.0F);
        matrices.scale(HUD_SCALE, HUD_SCALE, 1.0F);

        context.fill(0, 0, PANEL_WIDTH, PANEL_HEIGHT, 0xA0000000);
        context.drawBorder(0, 0, PANEL_WIDTH, PANEL_HEIGHT, 0x80FFFFFF);

        PlayerInventory inventory = client.player.getInventory();

        for (int row = 0; row < INVENTORY_ROWS; row++) {
            int rowY = PANEL_PADDING + HEADER_HEIGHT + (row * SLOT_SPACING);
            drawRow(context, client, inventory, FIRST_MAIN_INVENTORY_SLOT + (row * HOTBAR_SIZE), rowY, row == selectedRow, false);
        }

        int hotbarY = PANEL_PADDING + HEADER_HEIGHT + (INVENTORY_ROWS * SLOT_SPACING);
        drawRow(context, client, inventory, 0, hotbarY, false, true);

        matrices.pop();
    }

    private static void drawRow(DrawContext context, MinecraftClient client, PlayerInventory inventory, int startSlot, int y, boolean selected, boolean hotbar) {
        int rowColor = hotbar ? 0x4066A3FF : 0x30000000;
        int borderColor = hotbar ? 0xFF66A3FF : (selected ? 0xFFFFD54F : 0x40FFFFFF);

        context.fill(PANEL_PADDING - 2, y - 2, PANEL_PADDING + ROW_WIDTH + 2, y + SLOT_SIZE + 2, rowColor);
        context.drawBorder(PANEL_PADDING - 2, y - 2, ROW_WIDTH + 4, SLOT_SIZE + 4, borderColor);

        for (int column = 0; column < HOTBAR_SIZE; column++) {
            int slotX = PANEL_PADDING + (column * SLOT_SPACING);
            context.fill(slotX, y, slotX + SLOT_SIZE, y + SLOT_SIZE, 0x60202020);

            ItemStack stack = inventory.getStack(startSlot + column);
            if (!stack.isEmpty()) {
                context.drawItem(stack, slotX, y);
                context.drawItemInSlot(client.textRenderer, stack, slotX, y);
            }
        }
    }

    private static void swapSelectedRow(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) {
            return;
        }

        ScreenHandler handler = client.player.currentScreenHandler;
        if (handler == null || !handler.getCursorStack().isEmpty()) {
            return;
        }

        int sourceSlotStart = FIRST_MAIN_INVENTORY_SLOT + (selectedRow * HOTBAR_SIZE);
        for (int column = 0; column < HOTBAR_SIZE; column++) {
            client.interactionManager.clickSlot(
                    handler.syncId,
                    sourceSlotStart + column,
                    column,
                    SlotActionType.SWAP,
                    client.player
            );
        }
    }

    private static void clearSelection() {
        selectionActive = false;
        keyHeldLastTick = false;
    }
}
