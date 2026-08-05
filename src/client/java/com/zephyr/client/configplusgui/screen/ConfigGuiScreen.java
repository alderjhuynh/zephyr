package com.zephyr.client.configplusgui.screen;

import com.zephyr.client.configplusgui.config.GlobalConfig;
import com.zephyr.client.configplusgui.hud.ThemeColor;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.configplusgui.setting.Setting;
import com.zephyr.client.configplusgui.setting.StringSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Global settings tab. Rows are built from {@link Setting} instances and dispatched
 * on their concrete type (mirroring {@link ConfigManager}'s setting dispatch), so
 * new global settings are just another entry in {@link #computeRows()}. Theme row is
 * a special case (preset swatch); when "Use Custom Color" is on it is replaced by the
 * HSV slider group plus a live swatch. The sliders are the first drag interaction in
 * this tab: values update live while dragging and only save once on release.
 */
public final class ConfigGuiScreen extends ZephyrScreen {
    private static final int ROW_HEIGHT = 24;
    private static final int BOX_SIZE = 12;
    private static final int SLIDER_HEIGHT = 24;
    private static final int SWATCH_HEIGHT = 30;

    private NumberSetting draggingSetting = null;
    private boolean draggingDirty = false;

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
        if (row instanceof SettingRow settingRow) {
            renderSettingRow(graphics, settingRow.setting(), settingRow.top(), mouseX, mouseY);
        } else if (row instanceof ThemeRow themeRow) {
            renderThemeRow(graphics, themeRow.top(), mouseX, mouseY);
        } else if (row instanceof ColorRow colorRow) {
            renderColorRow(graphics, colorRow.top(), mouseX, mouseY);
        }
    }

    private void renderSettingRow(GuiGraphicsExtractor graphics, Setting<?> setting, int top, int mouseX, int mouseY) {
        if (setting instanceof BooleanSetting booleanSetting) {
            renderBooleanRow(graphics, booleanSetting, top, mouseX, mouseY);
        } else if (setting instanceof NumberSetting numberSetting) {
            renderSliderRow(graphics, setting.getName(), numberSetting, top, mouseX, mouseY);
        } else if (setting instanceof EnumSetting<?> enumSetting) {
            renderEnumRow(graphics, setting.getName(), enumSetting, top, mouseX, mouseY);
        } else if (setting instanceof StringSetting stringSetting) {
            renderStringRow(graphics, setting.getName(), stringSetting, top);
        }
    }

    private void renderBooleanRow(GuiGraphicsExtractor graphics, BooleanSetting setting, int top, int mouseX, int mouseY) {
        boolean enabled = setting.get();
        boolean hovered = isHovered(mouseX, mouseY, top);

        int bg = enabled ? rowBgEnabled() : (hovered ? ROW_BG_HOVER : ROW_BG);
        graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + ROW_HEIGHT, bg);

        int nameColor = enabled ? accent() : TEXT_MAIN;
        graphics.text(this.font, setting.getName(), panelX + PADDING + 8, top + 9, nameColor, false);

        int boxX = panelX + panelWidth - PADDING - 8 - BOX_SIZE;
        int boxY = top + (ROW_HEIGHT - BOX_SIZE) / 2;
        graphics.fill(boxX, boxY, boxX + BOX_SIZE, boxY + BOX_SIZE, enabled ? accent() : 0x40FFFFFF);
    }

    private void renderSliderRow(GuiGraphicsExtractor graphics, String label, NumberSetting setting, int top, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, top);
        graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + ROW_HEIGHT,
                hovered ? ROW_BG_HOVER : ROW_BG);

        String labelText = label + ": " + trimDouble(setting.get());
        graphics.text(this.font, labelText, panelX + PADDING + 8, top + 5, TEXT_MAIN, false);

        int left = panelX + PADDING + 8;
        int right = panelX + panelWidth - PADDING - 8;
        int trackY = top + 16;
        graphics.fill(left, trackY, right, trackY + 3, 0x40FFFFFF);
        int fillWidth = (int) ((right - left) * setting.getProgress());
        graphics.fill(left, trackY, left + fillWidth, trackY + 3, accent());
        graphics.fill(left + fillWidth - 1, trackY - 2, left + fillWidth + 1, trackY + 5, accent());
    }

    private void renderEnumRow(GuiGraphicsExtractor graphics, String label, EnumSetting<?> setting, int top, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, top);
        graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + ROW_HEIGHT,
                hovered ? ROW_BG_HOVER : ROW_BG);

        graphics.text(this.font, label, panelX + PADDING + 8, top + 9, TEXT_MAIN, false);

        String value = setting.getDisplayValue();
        int valueWidth = this.font.width(value);
        graphics.text(this.font, value, panelX + panelWidth - PADDING - 8 - valueWidth, top + 9, accent(), false);
    }

    private void renderStringRow(GuiGraphicsExtractor graphics, String label, StringSetting setting, int top) {
        graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + ROW_HEIGHT, ROW_BG);

        graphics.text(this.font, label, panelX + PADDING + 8, top + 9, TEXT_MAIN, false);

        String value = setting.get();
        int valueWidth = this.font.width(value);
        graphics.text(this.font, value, panelX + panelWidth - PADDING - 8 - valueWidth, top + 9, TEXT_DIM, false);
    }

    private void renderThemeRow(GuiGraphicsExtractor graphics, int top, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, top);

        graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + ROW_HEIGHT,
                hovered ? ROW_BG_HOVER : ROW_BG);

        graphics.text(this.font, "Theme Color", panelX + PADDING + 8, top + 9, TEXT_MAIN, false);

        ThemeColor color = GlobalConfig.themeColor();
        String value = color.displayName();
        int swatchSize = 10;
        int valueRight = panelX + panelWidth - PADDING - 8;
        int swatchX = valueRight - swatchSize;
        int swatchY = top + (ROW_HEIGHT - swatchSize) / 2;
        graphics.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize, color.accent());

        int valueWidth = this.font.width(value);
        graphics.text(this.font, value, valueRight - valueWidth - swatchSize - 6, top + 9, accent(), false);
    }

    private void renderColorRow(GuiGraphicsExtractor graphics, int top, int mouseX, int mouseY) {
        renderSliderRow(graphics, "Hue", GlobalConfig.customHue, top, mouseX, mouseY);
        renderSliderRow(graphics, "Saturation", GlobalConfig.customSaturation, top + SLIDER_HEIGHT, mouseX, mouseY);
        renderSliderRow(graphics, "Value", GlobalConfig.customValue, top + SLIDER_HEIGHT * 2, mouseX, mouseY);

        int swatchTop = top + SLIDER_HEIGHT * 3;
        boolean hovered = isHovered(mouseX, mouseY, swatchTop, SWATCH_HEIGHT);
        graphics.fill(panelX + PADDING, swatchTop, panelX + panelWidth - PADDING, swatchTop + SWATCH_HEIGHT,
                hovered ? ROW_BG_HOVER : ROW_BG);

        int left = panelX + PADDING + 8;
        int right = panelX + panelWidth - PADDING - 8;
        int accentColor = GlobalConfig.customAccent();
        graphics.fill(left, swatchTop + 6, left + 44, swatchTop + SWATCH_HEIGHT - 6, accentColor);

        String label = "Custom Accent";
        graphics.text(this.font, label, left + 52, swatchTop + (SWATCH_HEIGHT - 8) / 2, TEXT_MAIN, false);

        String hex = String.format("#%06X", accentColor & 0xFFFFFF);
        int hexWidth = this.font.width(hex);
        graphics.text(this.font, hex, right - hexWidth, swatchTop + (SWATCH_HEIGHT - 8) / 2, TEXT_DIM, false);
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
            if (rowContains(row, mouseX, mouseY)) {
                handleRowClick(row, mouseX, mouseY);
                return true;
            }
        }

        return false;
    }

    private void handleRowClick(Row row, double mouseX, double mouseY) {
        if (row instanceof SettingRow settingRow) {
            handleSettingClick(settingRow.setting(), mouseX);
        } else if (row instanceof ThemeRow) {
            GlobalConfig.cycleThemeColor();
        } else if (row instanceof ColorRow colorRow) {
            handleColorRowClick(colorRow, mouseX, mouseY);
        }
    }

    private void handleSettingClick(Setting<?> setting, double mouseX) {
        if (setting instanceof BooleanSetting booleanSetting) {
            if (booleanSetting == GlobalConfig.stealthMode) {
                // Stealth toggling has side effects (snapshot + force-disable/restore modules),
                // so it goes through the single choke point instead of the raw setting.
                GlobalConfig.toggleStealthMode();
            } else {
                booleanSetting.toggle();
                GlobalConfig.save();
            }
        } else if (setting instanceof NumberSetting numberSetting) {
            beginSliderDrag(numberSetting, mouseX);
        } else if (setting instanceof EnumSetting<?> enumSetting) {
            enumSetting.cycle();
            GlobalConfig.save();
        }
    }

    private void handleColorRowClick(ColorRow row, double mouseX, double mouseY) {
        for (int i = 0; i < 3; i++) {
            int sliderTop = row.top() + i * SLIDER_HEIGHT;
            if (mouseY >= sliderTop && mouseY < sliderTop + SLIDER_HEIGHT) {
                NumberSetting slider = switch (i) {
                    case 0 -> GlobalConfig.customHue;
                    case 1 -> GlobalConfig.customSaturation;
                    default -> GlobalConfig.customValue;
                };
                beginSliderDrag(slider, mouseX);
                return;
            }
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && draggingSetting != null) {
            updateSliderFromMouse(draggingSetting, event.x());
            draggingDirty = true;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && draggingSetting != null) {
            if (draggingDirty) {
                GlobalConfig.save();
            }
            draggingSetting = null;
            draggingDirty = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void beginSliderDrag(NumberSetting setting, double mouseX) {
        draggingSetting = setting;
        draggingDirty = false;
        updateSliderFromMouse(setting, mouseX);
    }

    private void updateSliderFromMouse(NumberSetting setting, double mouseX) {
        int left = panelX + PADDING + 8;
        int right = panelX + panelWidth - PADDING - 8;
        double progress = (mouseX - left) / (double) (right - left);
        setting.setFromProgress(progress);
    }

    private List<Row> computeRows() {
        List<Row> rows = new ArrayList<>();
        int cursor = panelY + headerHeight();

        rows.add(new SettingRow(GlobalConfig.hotkeyPopups, cursor));
        cursor += ROW_HEIGHT;

        rows.add(new SettingRow(GlobalConfig.useCustomColor, cursor));
        cursor += ROW_HEIGHT;

        if (GlobalConfig.useCustomColor.get()) {
            ColorRow colorRow = new ColorRow(cursor, SLIDER_HEIGHT * 3 + SWATCH_HEIGHT);
            rows.add(colorRow);
            cursor += colorRow.height();
        } else {
            rows.add(new ThemeRow(cursor));
            cursor += ROW_HEIGHT;
        }

        rows.add(new SettingRow(GlobalConfig.menuAnimationSpeed, cursor));
        cursor += ROW_HEIGHT;

        rows.add(new SettingRow(GlobalConfig.notificationCorner, cursor));
        cursor += ROW_HEIGHT;

        rows.add(new SettingRow(GlobalConfig.notificationLifetime, cursor));
        cursor += ROW_HEIGHT;

        rows.add(new SettingRow(GlobalConfig.hudMode, cursor));
        cursor += ROW_HEIGHT;

        rows.add(new SettingRow(GlobalConfig.autosaveInterval, cursor));
        cursor += ROW_HEIGHT;

        rows.add(new SettingRow(GlobalConfig.keybindConflictWarnings, cursor));
        cursor += ROW_HEIGHT;

        rows.add(new SettingRow(GlobalConfig.stealthMode, cursor));

        return rows;
    }

    private boolean rowContains(Row row, double mouseX, double mouseY) {
        if (mouseX < panelX + PADDING || mouseX > panelX + panelWidth - PADDING) return false;
        if (row instanceof ColorRow colorRow) {
            return mouseY >= colorRow.top() && mouseY < colorRow.top() + colorRow.height();
        }
        return mouseY >= row.top() && mouseY < row.top() + ROW_HEIGHT;
    }

    private boolean isHovered(double mouseX, double mouseY, int top) {
        return isHovered(mouseX, mouseY, top, ROW_HEIGHT);
    }

    private boolean isHovered(double mouseX, double mouseY, int top, int height) {
        return mouseX >= panelX + PADDING && mouseX <= panelX + panelWidth - PADDING
                && mouseY >= top && mouseY < top + height;
    }

    private static String trimDouble(double value) {
        return String.format("%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private sealed interface Row permits SettingRow, ThemeRow, ColorRow {
        int top();
    }

    private record SettingRow(Setting<?> setting, int top) implements Row {
    }

    private record ThemeRow(int top) implements Row {
    }

    private record ColorRow(int top, int height) implements Row {
    }
}
