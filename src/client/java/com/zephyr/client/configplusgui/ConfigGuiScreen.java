package com.zephyr.client.configplusgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class ConfigGuiScreen extends ZephyrScreen {
    private static final int ROW_HEIGHT = 24;
    private static final int BOX_SIZE = 12;

    public ConfigGuiScreen() {
        this(0);
    }

    ConfigGuiScreen(int enterDirection) {
        super(Component.literal("Zephyr"), enterDirection);
    }

    @Override
    protected Nav currentNav() {
        return Nav.CONFIG;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        withPanelSlide(() -> {
            renderChrome(graphics, mouseX, mouseY);

            for (Row row : computeRows()) {
                renderRow(graphics, row, mouseX, mouseY);
            }

            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        });
    }

    private void renderRow(GuiGraphicsExtractor graphics, Row row, int mouseX, int mouseY) {
        boolean enabled = row.getter.getAsBoolean();
        boolean hovered = mouseX >= panelX + PADDING && mouseX <= panelX + panelWidth - PADDING
                && mouseY >= row.top && mouseY < row.top + ROW_HEIGHT;

        int bg = enabled ? ROW_BG_ENABLED : (hovered ? ROW_BG_HOVER : ROW_BG);
        graphics.fill(panelX + PADDING, row.top, panelX + panelWidth - PADDING, row.top + ROW_HEIGHT, bg);

        int nameColor = enabled ? ACCENT : TEXT_MAIN;
        graphics.text(this.font, row.label, panelX + PADDING + 8, row.top + 9, nameColor, false);

        int boxX = panelX + panelWidth - PADDING - 8 - BOX_SIZE;
        int boxY = row.top + (ROW_HEIGHT - BOX_SIZE) / 2;
        graphics.fill(boxX, boxY, boxX + BOX_SIZE, boxY + BOX_SIZE, enabled ? ACCENT : 0x40FFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (button != 0) return false;

        for (Row row : computeRows()) {
            if (mouseY >= row.top && mouseY < row.top + ROW_HEIGHT
                    && mouseX >= panelX + PADDING && mouseX <= panelX + panelWidth - PADDING) {
                row.onToggle.run();
                return true;
            }
        }

        return false;
    }

    private List<Row> computeRows() {
        List<Row> rows = new ArrayList<>();
        int cursor = panelY + headerHeight();

        rows.add(new Row("Hotkey Popups", cursor,
                GlobalConfig::hotkeyPopupsEnabled, GlobalConfig::toggleHotkeyPopups));
        cursor += ROW_HEIGHT;

        return rows;
    }

    private record Row(String label, int top, BooleanSupplier getter, Runnable onToggle) {
    }
}