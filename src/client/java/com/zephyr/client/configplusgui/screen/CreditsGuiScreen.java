package com.zephyr.client.configplusgui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class CreditsGuiScreen extends ZephyrScreen {
    private static final int SECTION_HEIGHT = 20;
    private static final int ROW_HEIGHT = 24;

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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        withPanelSlide(() -> {
            renderChrome(graphics, mouseX, mouseY);

            int y = panelY + headerHeight();
            graphics.fill(panelX + PADDING, y, panelX + panelWidth - PADDING, y + ROW_HEIGHT, ROW_BG);
            graphics.drawString(this.font, "Made with love by Auraea", panelX + PADDING + 8, y + 9, TEXT_MAIN, false);
            y += ROW_HEIGHT;

            graphics.fill(panelX + PADDING, y, panelX + panelWidth - PADDING, y + ROW_HEIGHT, ROW_BG);
            graphics.drawString(this.font, "Thanks for using Zephyr!", panelX + PADDING + 8, y + 9, TEXT_DIM, false);

            super.render(graphics, mouseX, mouseY, partialTick);
        });
    }
}
