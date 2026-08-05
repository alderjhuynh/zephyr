package com.zephyr.client.configplusgui.screen;

import com.zephyr.client.configplusgui.keybind.KeybindManager;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.ModuleManager;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.configplusgui.setting.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
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

    private final List<Module> modules = new ArrayList<>(ModuleManager.getModules());
    private String searchQuery = "";
    private EditBox searchBox;

    private Category selectedCategory = null; // null == "All"
    private double scrollOffset = 0;
    private Module expandedModule = null;
    private NumberSetting draggingSetting = null;

    public ClickGuiScreen() {
        this(0);
    }

    ClickGuiScreen(int enterDirection) {
        super(Component.literal("Zephyr"), enterDirection);
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
        });
        this.addRenderableWidget(searchBox);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
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
                graphics.centeredText(this.font, "No modules found", panelX + panelWidth / 2,
                        listTop + 20, TEXT_DIM);
            }

            if (maxScroll > 0) {
                int trackHeight = listBottom - listTop;
                int barHeight = Math.max(20, trackHeight * trackHeight / (trackHeight + maxScroll));
                int barY = listTop + (int) ((trackHeight - barHeight) * (scrollOffset / (double) maxScroll));
                graphics.fill(panelX + panelWidth - 4, barY, panelX + panelWidth - 1, barY + barHeight, accentDim());
            }

            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        });
    }

    private void renderTabBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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
            graphics.text(this.font, tab.label, textX, tabY + 4, textColor, false);
        }
    }

    private void renderRow(GuiGraphicsExtractor graphics, RowLayout row, int mouseX, int mouseY, int scroll) {
        int top = row.top - scroll;
        boolean hovered = mouseX >= panelX + PADDING && mouseX <= panelX + panelWidth - PADDING
                && mouseY >= top && mouseY < top + ROW_HEIGHT;

        int bg = row.module.isEnabled() ? rowBgEnabled() : (hovered ? ROW_BG_HOVER : ROW_BG);
        graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + ROW_HEIGHT, bg);

        int nameColor = row.module.isEnabled() ? accent() : TEXT_MAIN;
        graphics.text(this.font, row.module.getName(), panelX + PADDING + 8, top + 9, nameColor, false);

        String tag = row.module.getCategory().getDisplayName();
        int tagWidth = this.font.width(tag);
        graphics.text(this.font, tag, panelX + panelWidth - PADDING - tagWidth - 8, top + 9, TEXT_DIM, false);

        if (row.expanded) {
            int settingTop = top + ROW_HEIGHT + 2;
            for (SettingRowLayout settingRow : row.settingRows) {
                renderSetting(graphics, settingRow, settingTop);
                settingTop += SETTING_ROW_HEIGHT;
            }
        }
    }

    private void renderSetting(GuiGraphicsExtractor graphics, SettingRowLayout settingRow, int top) {
        int left = panelX + PADDING + 8;
        int right = panelX + panelWidth - PADDING - 8;

        if (settingRow.setting instanceof BooleanSetting boolSetting) {
            graphics.text(this.font, settingRow.setting.getName(), left, top + 6, TEXT_DIM, false);
            int boxSize = 10;
            int boxX = right - boxSize;
            graphics.fill(boxX, top + 5, boxX + boxSize, top + 5 + boxSize,
                    boolSetting.get() ? accent() : 0x40FFFFFF);
        } else if (settingRow.setting instanceof NumberSetting numberSetting) {
            String label = settingRow.setting.getName() + ": " + trimDouble(numberSetting.get());
            graphics.text(this.font, label, left, top, TEXT_DIM, false);

            int trackY = top + 12;
            graphics.fill(left, trackY, right, trackY + 2, 0x40FFFFFF);
            int fillWidth = (int) ((right - left) * numberSetting.getProgress());
            graphics.fill(left, trackY, left + fillWidth, trackY + 2, accent());
            graphics.fill(left + fillWidth - 1, trackY - 2, left + fillWidth + 1, trackY + 4, accent());
        } else if (settingRow.setting instanceof EnumSetting<?> enumSetting) {
            graphics.text(this.font, settingRow.setting.getName(), left, top + 6, TEXT_DIM, false);

            String value = enumSetting.getDisplayValue();
            int valueWidth = this.font.width(value);
            graphics.text(this.font, value, right - valueWidth, top + 6, accent(), false);
        }
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
                return true;
            }

            if (row.expanded) {
                int settingTop = top + ROW_HEIGHT + 2;
                for (SettingRowLayout settingRow : row.settingRows) {
                    if (mouseY >= settingTop && mouseY < settingTop + SETTING_ROW_HEIGHT) {
                        handleSettingClick(settingRow, mouseX, settingTop);
                        return true;
                    }
                    settingTop += SETTING_ROW_HEIGHT;
                }
            }
        }

        return false;
    }

    private void handleSettingClick(SettingRowLayout settingRow, double mouseX, int settingTop) {
        if (settingRow.setting instanceof BooleanSetting boolSetting) {
            boolSetting.toggle();
        } else if (settingRow.setting instanceof NumberSetting numberSetting) {
            draggingSetting = numberSetting;
            updateSliderFromMouse(numberSetting, mouseX);
        } else if (settingRow.setting instanceof EnumSetting<?> enumSetting) {
            enumSetting.cycle();
        }
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
                    settingRows.add(new SettingRowLayout(setting));
                }
            }

            int height = ROW_HEIGHT + (expanded ? settingRows.size() * SETTING_ROW_HEIGHT + 2 : 0);
            layout.add(new RowLayout(module, cursor, height, expanded, settingRows));
            cursor += height;
        }

        return layout;
    }

    private record TabLayout(Category category, String label, int left, int right) {
    }

    private record SettingRowLayout(Setting<?> setting) {
    }

    private record RowLayout(Module module, int top, int height, boolean expanded, List<SettingRowLayout> settingRows) {
        int bottom() {
            return top + height;
        }
    }
}