package com.zephyr.client.module.qol.jade.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * A compact progress bar used for the entity health / armor rows. The bar sits to
 * the left and an optional trailing label (e.g. "10 / 20") to the right, mirroring
 * Jade's health/armor display without needing heart sprites.
 */
public class ProgressElement extends Element {
    public static final int BAR_WIDTH = 64;
    public static final int BAR_HEIGHT = 4;

    private final Component text;
    private final float progress;
    private final int barColor;
    private final int backgroundColor;
    private final int textColor;

    /**
     * Creates a progress bar element. The progress is clamped to [0, 1], and the
     * width grows to fit an optional trailing label.
     *
     * @param text            the trailing label, or {@code null} for no label
     * @param progress        the filled fraction of the bar (0..1)
     * @param barColor        the ARGB color of the filled portion
     * @param backgroundColor the ARGB color of the unfilled background
     * @param textColor       the ARGB color of the label text
     */
    public ProgressElement(Component text, float progress, int barColor, int backgroundColor, int textColor) {
        this.text = text;
        this.progress = Math.max(0, Math.min(1, progress));
        this.barColor = barColor;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.width = BAR_WIDTH;
        this.height = BAR_HEIGHT;
        if (text != null) {
            this.width += 4 + font().width(text);
            this.height = Math.max(BAR_HEIGHT, font().lineHeight);
        }
    }

    /** Draws the background bar, filled portion, and label (if any). */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int barY = y + (height - BAR_HEIGHT) / 2;
        graphics.fill(x, barY, x + BAR_WIDTH, barY + BAR_HEIGHT, ARGB.multiplyAlpha(backgroundColor, alpha));
        if (progress > 0) {
            graphics.fill(x, barY, x + (int) (BAR_WIDTH * progress), barY + BAR_HEIGHT,
                    ARGB.multiplyAlpha(barColor, alpha));
        }
        if (text != null) {
            graphics.text(font(), text, x + BAR_WIDTH + 4, y, ARGB.multiplyAlpha(textColor, alpha), true);
        }
    }
}
