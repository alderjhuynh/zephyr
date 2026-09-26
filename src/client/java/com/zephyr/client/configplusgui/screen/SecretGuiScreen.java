package com.zephyr.client.configplusgui.screen;

import com.zephyr.client.configplusgui.secretsettings.bettermovement.BetterMovement;
import com.zephyr.client.configplusgui.hud.PartyManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The hidden "???" easter-egg screen (reachable only via the vertical Cycle Screen cycle,
 * see {@link ZephyrScreen#advance}). Shows the party-mode toggles backed by
 * {@link PartyManager} (rainbow, uwu chat, confetti, wobble, module roulette), the hidden
 * {@link BetterMovement} toggle, and session statistics (uwu'd messages, roulette spins).
 * The bottom-right indicator renders as "???" instead of the normal nav name.
 */
public final class SecretGuiScreen extends ZephyrScreen {
    private static final int SECTION_HEIGHT = 20;
    private static final int ROW_HEIGHT = 24;
    private static final int BOX_SIZE = 12;

    private double scrollOffset = 0;

    /** Creates the secret screen with a vertical slide; package-visible for {@link ZephyrScreen.Nav}. */
    SecretGuiScreen(int enterDirection) {
        super(Component.literal("Zephyr"), enterDirection, true);
    }

    @Override
    protected Nav currentNav() {
        return Nav.SECRET;
    }

    @Override
    protected String indicatorText() {
        return "???";
    }

    /** Renders the chrome, section headers, toggle rows and stat rows in a scrolled list. */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        withPanelSlide(() -> {
            renderChrome(graphics, mouseX, mouseY);

            int listTop = panelY + headerHeight();
            int listBottom = panelY + panelHeight - PADDING;

            List<Row> rows = computeRows();
            int contentHeight = rows.isEmpty() ? 0 : rows.get(rows.size() - 1).bottom() - listTop;
            int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            graphics.enableScissor(panelX, listTop, panelX + panelWidth, listBottom);
            for (Row row : rows) {
                renderRow(graphics, row, (int) scrollOffset, mouseX, mouseY);
            }
            graphics.disableScissor();

            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        });
    }

    /** Draws a row according to its type: header, toggle or stat. */
    private void renderRow(GuiGraphicsExtractor graphics, Row row, int scroll, int mouseX, int mouseY) {
        int top = row.top() - scroll;
        if (row instanceof SectionHeader header) {
            graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + SECTION_HEIGHT, TAB_BG);
            graphics.text(this.font, header.label, panelX + PADDING + 8, top + 7, TEXT_DIM, false);
        } else if (row instanceof ToggleRow toggle) {
            renderToggle(graphics, toggle, top, mouseX, mouseY);
        } else if (row instanceof StatRow stat) {
            renderStat(graphics, stat, top);
        }
    }

    /** Draws a checkbox toggle row with accent fill when enabled. */
    private void renderToggle(GuiGraphicsExtractor graphics, ToggleRow toggle, int top, int mouseX, int mouseY) {
        boolean enabled = toggle.get();
        boolean hovered = mouseX >= panelX + PADDING && mouseX <= panelX + panelWidth - PADDING
                && mouseY >= top && mouseY < top + ROW_HEIGHT;

        int bg = enabled ? rowBgEnabled() : (hovered ? ROW_BG_HOVER : ROW_BG);
        graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + ROW_HEIGHT, bg);

        int nameColor = enabled ? accent() : TEXT_MAIN;
        graphics.text(this.font, toggle.label, panelX + PADDING + 8, top + 9, nameColor, false);

        int boxX = panelX + panelWidth - PADDING - 8 - BOX_SIZE;
        int boxY = top + (ROW_HEIGHT - BOX_SIZE) / 2;
        graphics.fill(boxX, boxY, boxX + BOX_SIZE, boxY + BOX_SIZE, enabled ? accent() : 0x40FFFFFF);
    }

    /** Draws a read-only session-stat row with its numeric value in accent. */
    private void renderStat(GuiGraphicsExtractor graphics, StatRow stat, int top) {
        graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + ROW_HEIGHT, ROW_BG);
        graphics.text(this.font, stat.label, panelX + PADDING + 8, top + 9, TEXT_DIM, false);

        String value = Integer.toString(stat.get());
        int valueWidth = this.font.width(value);
        graphics.text(this.font, value, panelX + panelWidth - PADDING - 8 - valueWidth, top + 9, accent(), false);
    }

    /** Toggles the clicked toggle row; headers and stats are not interactive. */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (button != 0) return false;

        int scroll = (int) scrollOffset;
        for (Row row : computeRows()) {
            if (!(row instanceof ToggleRow toggle)) continue;
            int top = row.top() - scroll;
            if (mouseY < top || mouseY >= top + ROW_HEIGHT) continue;
            if (mouseX < panelX + PADDING || mouseX > panelX + panelWidth - PADDING) continue;

            toggle.toggle();
            return true;
        }

        return false;
    }

    /** Scrolls the secret list by the wheel delta. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= scrollY * ROW_HEIGHT;
        return true;
    }

    /** Builds the row list: party toggles, secret features, then session stats. */
    private List<Row> computeRows() {
        List<Row> rows = new ArrayList<>();
        int cursor = panelY + headerHeight();

        rows.add(new SectionHeader("PARTY MODE", cursor));
        cursor += SECTION_HEIGHT;

        rows.add(new ToggleRow("Rainbow", () -> PartyManager.rainbow, value -> PartyManager.rainbow = value, cursor));
        cursor += ROW_HEIGHT;
        rows.add(new ToggleRow("UwU Chat", () -> PartyManager.uwu, value -> PartyManager.uwu = value, cursor));
        cursor += ROW_HEIGHT;
        rows.add(new ToggleRow("Confetti Toasts", () -> PartyManager.confetti, value -> PartyManager.confetti = value, cursor));
        cursor += ROW_HEIGHT;
        rows.add(new ToggleRow("Wobble", () -> PartyManager.wobble, value -> PartyManager.wobble = value, cursor));
        cursor += ROW_HEIGHT;
        rows.add(new ToggleRow("Module Roulette", () -> PartyManager.roulette, PartyManager::setRoulette, cursor));
        cursor += ROW_HEIGHT;

        rows.add(new SectionHeader("SECRET FEATURES", cursor));
        cursor += SECTION_HEIGHT;

        rows.add(new ToggleRow("Better Movement", () -> BetterMovement.enabled, BetterMovement::setEnabled, cursor));
        cursor += ROW_HEIGHT;

        rows.add(new SectionHeader("SESSION STATS", cursor));
        cursor += SECTION_HEIGHT;

        rows.add(new StatRow("UwU'd Messages", PartyManager::uwuCount, cursor));
        cursor += ROW_HEIGHT;
        rows.add(new StatRow("Roulette Spins", PartyManager::rouletteSpins, cursor));

        return rows;
    }

    private sealed interface Row permits SectionHeader, ToggleRow, StatRow {
        int top();

        int bottom();
    }

    private record SectionHeader(String label, int top) implements Row {
        @Override
        public int bottom() {
            return top + SECTION_HEIGHT;
        }
    }

    private record ToggleRow(String label, BooleanGetter getter, BooleanSetter setter, int top) implements Row {
        boolean get() {
            return getter.get();
        }

        void toggle() {
            setter.set(!get());
        }

        @Override
        public int bottom() {
            return top + ROW_HEIGHT;
        }
    }

    private record StatRow(String label, IntGetter getter, int top) implements Row {
        int get() {
            return getter.get();
        }

        @Override
        public int bottom() {
            return top + ROW_HEIGHT;
        }
    }

    @FunctionalInterface
    private interface BooleanGetter {
        boolean get();
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }

    @FunctionalInterface
    private interface IntGetter {
        int get();
    }
}
