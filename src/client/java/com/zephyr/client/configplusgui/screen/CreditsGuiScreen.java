package com.zephyr.client.configplusgui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * One of the two hidden "easter egg" screens (alongside {@link SecretGuiScreen}), reachable
 * only by holding Up/Down while pressing the Cycle Screen keybind. Shows the mod's author
 * credit and a thank-you message on a simple two-row panel. Enters and exits vertically
 * like the rest of the hidden cycle.
 */
public final class CreditsGuiScreen extends ZephyrScreen {
    private static final int SECTION_HEIGHT = 20;
    private static final int ROW_HEIGHT = 24;

    /** Creates the credits screen with a vertical slide; package-visible for {@link ZephyrScreen.Nav}. */
    CreditsGuiScreen(int enterDirection) {
        super(Component.literal("Zephyr"), enterDirection, true);
    }

    @Override
    protected Nav currentNav() {
        return Nav.CREDITS;
    }

    @Override
    protected String indicatorText() {
        return "CREDITS";
    }

    /** Renders the two credit rows: author line and thank-you line. */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        withPanelSlide(() -> {
            renderChrome(graphics, mouseX, mouseY);

            int y = panelY + headerHeight();
            graphics.fill(panelX + PADDING, y, panelX + panelWidth - PADDING, y + ROW_HEIGHT, ROW_BG);
            graphics.text(this.font, "Made with love by Auraea", panelX + PADDING + 8, y + 9, TEXT_MAIN, false);
            y += ROW_HEIGHT;

            graphics.fill(panelX + PADDING, y, panelX + panelWidth - PADDING, y + ROW_HEIGHT, ROW_BG);
            graphics.text(this.font, "Thanks for using Zephyr!", panelX + PADDING + 8, y + 9, TEXT_DIM, false);

            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        });
    }
}
