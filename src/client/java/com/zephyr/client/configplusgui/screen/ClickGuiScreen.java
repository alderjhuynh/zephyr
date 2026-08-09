package com.zephyr.client.configplusgui.screen;

import com.zephyr.client.configplusgui.keybind.KeybindManager;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.ModuleManager;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.setting.ListSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.configplusgui.setting.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Lunar-esque click-gui: a blurred, scrollable, searchable list of every registered
 * {@link Module}. Left-click a row to toggle it, right click to expand its settings.
 * Opened/closed via the "Open Menu" keybind (L + Enter by default, see
 * {@link KeybindManager}), and reachable from the other two Zephyr screens with the
 * "Cycle Screen" keybind (Tab by default).
 */
public final class ClickGuiScreen extends ZephyrScreen {
    private static final int TAB_HEIGHT = 18;
    private static final int SEARCH_HEIGHT = 20;
    private static final int ROW_HEIGHT = 26;
    private static final int SETTING_ROW_HEIGHT = 20;
    private static final int LIST_ICON_SIZE = 10;

    private final List<Module> modules = new ArrayList<>(ModuleManager.getModules());
    private String searchQuery = "";
    private EditBox searchBox;

    private Category selectedCategory = null; // null == "All"
    private double scrollOffset = 0;
    private Module expandedModule = null;
    private NumberSetting draggingSetting = null;

    /** List setting whose "add entry" row is currently open, plus its two text inputs. */
    private ListSetting listEditing = null;
    private EditBox blockInputBox;
    private EditBox colorInputBox;

    public ClickGuiScreen() {
        this(0, false);
    }

    ClickGuiScreen(int enterDirection, boolean slideVertically) {
        super(Component.literal("Zephyr"), enterDirection, slideVertically);
    }

    @Override
    protected Nav currentNav() {
        return Nav.MAIN;
    }

    @Override
    protected int headerHeight() {
        return TITLE_HEIGHT + TAB_HEIGHT + SEARCH_HEIGHT;
    }

    @Override
    protected void initWidgets() {
        int searchY = panelY + TITLE_HEIGHT + TAB_HEIGHT + 2;
        searchBox = new EditBox(this.font, panelX + PADDING, searchY, panelWidth - PADDING * 2, 16,
                Component.literal("Search"));
        searchBox.setHint(Component.literal("Search modules..."));
        searchBox.setBordered(false);
        searchBox.setResponder(query -> {
            this.searchQuery = query;
            this.scrollOffset = 0;
            clearListEditing();
        });
        this.addRenderableWidget(searchBox);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        withPanelSlide(() -> {
            // The search box's x/y were fixed at init() time; keep it tracking the
            // panel while a slide-cycle animation is temporarily offsetting panelX.
            searchBox.setX(panelX + PADDING);
            searchBox.setY(panelY + TITLE_HEIGHT + TAB_HEIGHT + 2);

            renderChrome(graphics, mouseX, mouseY);
            renderTabBar(graphics, mouseX, mouseY);

            int listTop = panelY + headerHeight();
            int listBottom = panelY + panelHeight - PADDING;

            List<RowLayout> layout = computeLayout(listTop);
            int contentHeight = layout.isEmpty() ? 0
                    : (layout.get(layout.size() - 1).bottom() - listTop);
            int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            graphics.enableScissor(panelX, listTop, panelX + panelWidth, listBottom);
            for (RowLayout row : layout) {
                renderRow(graphics, row, mouseX, mouseY, (int) scrollOffset);
            }
            graphics.disableScissor();

            if (layout.isEmpty()) {
                graphics.drawCenteredString(this.font, "No modules found", panelX + panelWidth / 2,
                        listTop + 20, TEXT_DIM);
            }

            if (maxScroll > 0) {
                int trackHeight = listBottom - listTop;
                int barHeight = Math.max(20, trackHeight * trackHeight / (trackHeight + maxScroll));
                int barY = listTop + (int) ((trackHeight - barHeight) * (scrollOffset / (double) maxScroll));
                graphics.fill(panelX + panelWidth - 4, barY, panelX + panelWidth - 1, barY + barHeight, accentDim());
            }

            super.render(graphics, mouseX, mouseY, partialTick);
        });
    }

    private void renderTabBar(GuiGraphics graphics, int mouseX, int mouseY) {
        int tabY = panelY + TITLE_HEIGHT;
        for (TabLayout tab : computeTabLayout()) {
            boolean selected = tab.category == selectedCategory;
            boolean hovered = !selected && mouseX >= tab.left && mouseX < tab.right
                    && mouseY >= tabY && mouseY < tabY + TAB_HEIGHT - 2;

            int bg = selected ? accent() : (hovered ? ROW_BG_HOVER : TAB_BG);
            graphics.fill(tab.left, tabY, tab.right, tabY + TAB_HEIGHT - 2, bg);

            int textColor = selected ? TEXT_ON_ACCENT : TEXT_DIM;
            int textWidth = this.font.width(tab.label);
            int textX = tab.left + (tab.right - tab.left - textWidth) / 2;
            graphics.drawString(this.font, tab.label, textX, tabY + 4, textColor, false);
        }
    }

    private void renderRow(GuiGraphics graphics, RowLayout row, int mouseX, int mouseY, int scroll) {
        int top = row.top - scroll;
        boolean hovered = mouseX >= panelX + PADDING && mouseX <= panelX + panelWidth - PADDING
                && mouseY >= top && mouseY < top + ROW_HEIGHT;

        int bg = row.module.isEnabled() ? rowBgEnabled() : (hovered ? ROW_BG_HOVER : ROW_BG);
        graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + ROW_HEIGHT, bg);

        int nameColor = row.module.isEnabled() ? accent() : TEXT_MAIN;
        graphics.drawString(this.font, row.module.getName(), panelX + PADDING + 8, top + 9, nameColor, false);

        String tag = row.module.getCategory().getDisplayName();
        int tagWidth = this.font.width(tag);
        graphics.drawString(this.font, tag, panelX + panelWidth - PADDING - tagWidth - 8, top + 9, TEXT_DIM, false);

        if (row.expanded) {
            int settingTop = top + ROW_HEIGHT + 2;
            for (SettingRowLayout settingRow : row.settingRows) {
                renderSetting(graphics, settingRow, settingTop);
                settingTop += settingRow.height();
            }
        }
    }

    private void renderSetting(GuiGraphics graphics, SettingRowLayout settingRow, int top) {
        int left = panelX + PADDING + 8;
        int right = panelX + panelWidth - PADDING - 8;

        if (settingRow.setting instanceof BooleanSetting boolSetting) {
            graphics.drawString(this.font, settingRow.setting.getName(), left, top + 6, TEXT_DIM, false);
            int boxSize = 10;
            int boxX = right - boxSize;
            graphics.fill(boxX, top + 5, boxX + boxSize, top + 5 + boxSize,
                    boolSetting.get() ? accent() : 0x40FFFFFF);
        } else if (settingRow.setting instanceof NumberSetting numberSetting) {
            String label = settingRow.setting.getName() + ": " + trimDouble(numberSetting.get());
            graphics.drawString(this.font, label, left, top, TEXT_DIM, false);

            int trackY = top + 12;
            graphics.fill(left, trackY, right, trackY + 2, 0x40FFFFFF);
            int fillWidth = (int) ((right - left) * numberSetting.getProgress());
            graphics.fill(left, trackY, left + fillWidth, trackY + 2, accent());
            graphics.fill(left + fillWidth - 1, trackY - 2, left + fillWidth + 1, trackY + 4, accent());
        } else if (settingRow.setting instanceof EnumSetting<?> enumSetting) {
            graphics.drawString(this.font, settingRow.setting.getName(), left, top + 6, TEXT_DIM, false);

            String value = enumSetting.getDisplayValue();
            int valueWidth = this.font.width(value);
            graphics.drawString(this.font, value, right - valueWidth, top + 6, accent(), false);
        } else if (settingRow.setting instanceof ListSetting listSetting) {
            renderListSetting(graphics, listSetting, top, left, right);
        }
    }

    private void renderListSetting(GuiGraphics graphics, ListSetting setting, int top, int left, int right) {
        graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + SETTING_ROW_HEIGHT, ROW_BG_HOVER);
        graphics.drawString(this.font, setting.getName(), left, top + 6, TEXT_DIM, false);

        int addX = right - LIST_ICON_SIZE;
        int addY = top + (SETTING_ROW_HEIGHT - LIST_ICON_SIZE) / 2;
        graphics.fill(addX, addY, addX + LIST_ICON_SIZE, addY + LIST_ICON_SIZE, accent());
        drawPlus(graphics, addX, addY, TEXT_ON_ACCENT);

        int entryTop = top + SETTING_ROW_HEIGHT;
        for (ListSetting.ListEntry entry : setting.get()) {
            graphics.fill(panelX + PADDING, entryTop, panelX + panelWidth - PADDING, entryTop + SETTING_ROW_HEIGHT, ROW_BG);

            int swatchX = left;
            int swatchY = entryTop + (SETTING_ROW_HEIGHT - 8) / 2;
            graphics.fill(swatchX, swatchY, swatchX + 8, swatchY + 8, ListSetting.parseColor(entry.color(), 0xFFFFFFFF));

            graphics.drawString(this.font, entry.blockName(), left + 14, entryTop + 6, TEXT_MAIN, false);

            int removeX = right - LIST_ICON_SIZE;
            int removeY = entryTop + (SETTING_ROW_HEIGHT - LIST_ICON_SIZE) / 2;
            graphics.fill(removeX, removeY, removeX + LIST_ICON_SIZE, removeY + LIST_ICON_SIZE, 0xFFE05B5B);
            graphics.drawCenteredString(this.font, "x", removeX + LIST_ICON_SIZE / 2, removeY, TEXT_DIM);

            entryTop += SETTING_ROW_HEIGHT;
        }

        if (listEditing == setting) {
            renderListInputRow(graphics, setting, entryTop, left, right);
        }
    }

    private void renderListInputRow(GuiGraphics graphics, ListSetting setting, int top, int left, int right) {
        graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + SETTING_ROW_HEIGHT, ROW_BG_HOVER);

        int blockWidth = 90;
        int colorWidth = 60;
        int gap = 4;
        int commitX = right - LIST_ICON_SIZE;

        blockInputBox.setX(left);
        blockInputBox.setY(top + 4);
        blockInputBox.setWidth(blockWidth);
        colorInputBox.setX(left + blockWidth + gap);
        colorInputBox.setY(top + 4);
        colorInputBox.setWidth(colorWidth);

        int commitY = top + (SETTING_ROW_HEIGHT - LIST_ICON_SIZE) / 2;
        graphics.fill(commitX, commitY, commitX + LIST_ICON_SIZE, commitY + LIST_ICON_SIZE, accent());
        drawPlus(graphics, commitX, commitY, TEXT_ON_ACCENT);
    }

    private static void drawPlus(GuiGraphics graphics, int x, int y, int color) {
        int cx = x + LIST_ICON_SIZE / 2;
        int cy = y + LIST_ICON_SIZE / 2;
        graphics.fill(cx - 3, cy - 1, cx + 3, cy + 1, color);
        graphics.fill(cx - 1, cy - 3, cx + 1, cy + 3, color);
    }

    private static String trimDouble(double value) {
        return String.format("%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0) {
            int tabY = panelY + TITLE_HEIGHT;
            if (mouseY >= tabY && mouseY < tabY + TAB_HEIGHT - 2) {
                for (TabLayout tab : computeTabLayout()) {
                    if (mouseX >= tab.left && mouseX < tab.right) {
                        selectedCategory = tab.category;
                        scrollOffset = 0;
                        clearListEditing();
                        return true;
                    }
                }
            }
        }

        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        int listTop = panelY + headerHeight();
        List<RowLayout> layout = computeLayout(listTop);
        int scroll = (int) scrollOffset;

        for (RowLayout row : layout) {
            int top = row.top - scroll;

            if (mouseY >= top && mouseY < top + ROW_HEIGHT
                    && mouseX >= panelX + PADDING && mouseX <= panelX + panelWidth - PADDING) {
                if (button == 0) {
                    row.module.toggle();
                } else if (button == 1) {
                    expandedModule = (expandedModule == row.module) ? null : row.module;
                }
                clearListEditing();
                return true;
            }

            if (row.expanded) {
                int settingTop = top + ROW_HEIGHT + 2;
                for (SettingRowLayout settingRow : row.settingRows) {
                    if (mouseY >= settingTop && mouseY < settingTop + settingRow.height()) {
                        handleSettingClick(settingRow, mouseX, mouseY, settingTop);
                        return true;
                    }
                    settingTop += settingRow.height();
                }
            }
        }

        return false;
    }

    private void handleSettingClick(SettingRowLayout settingRow, double mouseX, double mouseY, int settingTop) {
        if (settingRow.setting instanceof BooleanSetting boolSetting) {
            boolSetting.toggle();
        } else if (settingRow.setting instanceof NumberSetting numberSetting) {
            draggingSetting = numberSetting;
            updateSliderFromMouse(numberSetting, mouseX);
        } else if (settingRow.setting instanceof EnumSetting<?> enumSetting) {
            enumSetting.cycle();
        } else if (settingRow.setting instanceof ListSetting listSetting) {
            handleListSettingClick(listSetting, mouseX, mouseY, settingTop);
        }
    }

    private void handleListSettingClick(ListSetting setting, double mouseX, double mouseY, int top) {
        int left = panelX + PADDING + 8;
        int right = panelX + panelWidth - PADDING - 8;

        if (mouseY < top + SETTING_ROW_HEIGHT) {
            int addX = right - LIST_ICON_SIZE;
            int addY = top + (SETTING_ROW_HEIGHT - LIST_ICON_SIZE) / 2;
            if (mouseX >= addX && mouseX < addX + LIST_ICON_SIZE
                    && mouseY >= addY && mouseY < addY + LIST_ICON_SIZE) {
                startAddingEntry(setting);
            }
            return;
        }

        int entryTop = top + SETTING_ROW_HEIGHT;
        int entryIndex = (int) ((mouseY - entryTop) / SETTING_ROW_HEIGHT);
        if (entryIndex >= 0 && entryIndex < setting.get().size()) {
            int rowTop = entryTop + entryIndex * SETTING_ROW_HEIGHT;
            int removeX = right - LIST_ICON_SIZE;
            int removeY = rowTop + (SETTING_ROW_HEIGHT - LIST_ICON_SIZE) / 2;
            if (mouseX >= removeX && mouseX < removeX + LIST_ICON_SIZE
                    && mouseY >= removeY && mouseY < removeY + LIST_ICON_SIZE) {
                setting.remove(entryIndex);
            }
            return;
        }

        if (listEditing == setting) {
            int inputTop = top + SETTING_ROW_HEIGHT * (1 + setting.get().size());
            int commitX = right - LIST_ICON_SIZE;
            int commitY = inputTop + (SETTING_ROW_HEIGHT - LIST_ICON_SIZE) / 2;
            if (mouseX >= commitX && mouseX < commitX + LIST_ICON_SIZE
                    && mouseY >= commitY && mouseY < commitY + LIST_ICON_SIZE) {
                commitNewEntry();
            }
        }
    }

    private void startAddingEntry(ListSetting setting) {
        cancelListEditing();
        listEditing = setting;
        blockInputBox = new EditBox(this.font, 0, 0, 90, 12, Component.literal("Block id"));
        blockInputBox.setHint(Component.literal("block id"));
        blockInputBox.setBordered(false);
        blockInputBox.setMaxLength(64);
        colorInputBox = new EditBox(this.font, 0, 0, 60, 12, Component.literal("Color"));
        colorInputBox.setHint(Component.literal("#rrggbb"));
        colorInputBox.setBordered(false);
        colorInputBox.setMaxLength(9);
        addRenderableWidget(blockInputBox);
        addRenderableWidget(colorInputBox);
        blockInputBox.setFocused(true);
        setFocused(blockInputBox);
    }

    private void commitNewEntry() {
        if (listEditing != null) {
            String blockName = blockInputBox != null ? blockInputBox.getValue().trim() : "";
            String color = colorInputBox != null ? colorInputBox.getValue().trim() : "";
            if (!blockName.isEmpty()) {
                listEditing.add(blockName, color.isEmpty() ? "#FFFFFF" : color);
            }
        }
        cancelListEditing();
    }

    private void cancelListEditing() {
        if (blockInputBox != null) {
            if (getFocused() == blockInputBox || getFocused() == colorInputBox) {
                setFocused(null);
            }
            removeWidget(blockInputBox);
            removeWidget(colorInputBox);
        }
        blockInputBox = null;
        colorInputBox = null;
        listEditing = null;
    }

    private void clearListEditing() {
        if (listEditing != null) {
            cancelListEditing();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isConfirmation() && listEditing != null) {
            commitNewEntry();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        int button = event.button();
        if (button == 0 && draggingSetting != null) {
            updateSliderFromMouse(draggingSetting, mouseX);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        int button = event.button();
        if (button == 0 && draggingSetting != null) {
            draggingSetting = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void updateSliderFromMouse(NumberSetting setting, double mouseX) {
        int left = panelX + PADDING + 8;
        int right = panelX + panelWidth - PADDING - 8;
        double progress = (mouseX - left) / (double) (right - left);
        setting.setFromProgress(progress);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= scrollY * ROW_HEIGHT;
        return true;
    }

    private List<TabLayout> computeTabLayout() {
        List<TabLayout> tabs = new ArrayList<>();
        Category[] categories = Category.values();
        int tabCount = categories.length + 1; // +1 for "All"
        int tabWidth = (panelWidth - PADDING * 2) / tabCount;
        int x = panelX + PADDING;

        tabs.add(new TabLayout(null, "All", x, x + tabWidth));
        x += tabWidth;

        for (Category category : categories) {
            tabs.add(new TabLayout(category, category.getDisplayName(), x, x + tabWidth));
            x += tabWidth;
        }

        return tabs;
    }

    private List<RowLayout> computeLayout(int listTop) {
        List<RowLayout> layout = new ArrayList<>();
        String query = searchQuery.toLowerCase();
        int cursor = listTop;

        for (Module module : modules) {
            if (selectedCategory != null && module.getCategory() != selectedCategory) {
                continue;
            }
            if (!query.isBlank() && !module.getName().toLowerCase().contains(query)
                    && !module.getCategory().getDisplayName().toLowerCase().contains(query)) {
                continue;
            }

            boolean expanded = module == expandedModule && !module.getSettings().isEmpty();
            List<SettingRowLayout> settingRows = new ArrayList<>();
            if (expanded) {
                for (Setting<?> setting : module.getSettings()) {
                    settingRows.add(new SettingRowLayout(setting, settingHeight(setting)));
                }
            }

            int height = ROW_HEIGHT + (expanded ? settingRows.stream().mapToInt(SettingRowLayout::height).sum() + 2 : 0);
            layout.add(new RowLayout(module, cursor, height, expanded, settingRows));
            cursor += height;
        }

        return layout;
    }

    private int settingHeight(Setting<?> setting) {
        if (setting instanceof ListSetting listSetting) {
            return SETTING_ROW_HEIGHT * (1 + listSetting.get().size() + (listEditing == listSetting ? 1 : 0));
        }
        return SETTING_ROW_HEIGHT;
    }

    private record TabLayout(Category category, String label, int left, int right) {
    }

    private record SettingRowLayout(Setting<?> setting, int height) {
    }

    private record RowLayout(Module module, int top, int height, boolean expanded, List<SettingRowLayout> settingRows) {
        int bottom() {
            return top + height;
        }
    }
}