package com.zephyr.client.module.qol.jade.render;

import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;

import java.util.List;

/**
 * The root container drawn by the overlay: a bordered, semi-transparent panel that
 * lays out the tooltip's icon (left) and rows of elements (right), then renders
 * the whole thing with a single alpha. Theming follows Zephyr's own HUD panels
 * (dark background, accent border) rather than Jade's green theme.
 */
public class BoxElement {
    public static final int PADDING = 4;
    public static final int BORDER = 1;
    private static final int ELEMENT_GAP = 3;
    private static final int ICON_GAP = 3;

    private final Tooltip tooltip;
    private int width;
    private int height;

    private int borderColor;
    private int backgroundColor;

    /**
     * Creates a box for the given tooltip, using the default dark theme until
     * {@link #setTheme} is called.
     *
     * @param tooltip the tooltip model whose lines and icon this box renders
     */
    public BoxElement(Tooltip tooltip) {
        this.tooltip = tooltip;
        this.borderColor = 0xFF2E3440;
        this.backgroundColor = 0xE0141018;
    }

    /** @return the tooltip model backing this box */
    public Tooltip getTooltip() {
        return tooltip;
    }

    /** @return the measured box width in GUI pixels */
    public int getWidth() {
        return width;
    }

    /** @return the measured box height in GUI pixels */
    public int getHeight() {
        return height;
    }

    /** Sets the border/bg colors from raw ARGB values. */
    public void setTheme(int borderColor, int backgroundColor) {
        this.borderColor = borderColor;
        this.backgroundColor = backgroundColor;
    }

    /** Measures the tooltip and caches the box dimensions (call before rendering). */
    public void layout() {
        int contentWidth = 0;
        int contentHeight = 0;
        for (Tooltip.Line line : tooltip.lines()) {
            int lineWidth = 0;
            int lineHeight = 0;
            List<Element> elements = line.elements();
            for (int i = 0; i < elements.size(); i++) {
                Element element = elements.get(i);
                lineWidth += element.width;
                lineHeight = Math.max(lineHeight, element.height);
                if (i < elements.size() - 1) {
                    lineWidth += ELEMENT_GAP;
                }
            }
            contentWidth = Math.max(contentWidth, lineWidth);
            contentHeight += lineHeight + line.marginBottom();
        }
        Element icon = tooltip.getIcon();
        if (icon != null) {
            contentWidth += icon.width + ICON_GAP;
            contentHeight = Math.max(contentHeight, icon.height);
        }
        this.width = contentWidth + PADDING * 2 + BORDER * 2;
        this.height = contentHeight + PADDING * 2 + BORDER * 2;
    }

    /**
     * Draws the box at (x, y) with the given fade alpha (0..1), delegating the
     * positioning and drawing of each contained element.
     */
    public void render(GuiGraphicsExtractor graphics, int x, int y, float alpha) {
        graphics.fill(x, y, x + width, y + height, ARGB.multiplyAlpha(borderColor, alpha));
        graphics.fill(x + BORDER, y + BORDER, x + width - BORDER, y + height - BORDER,
                ARGB.multiplyAlpha(backgroundColor, alpha));

        int innerX = x + BORDER + PADDING;
        int innerY = y + BORDER + PADDING;
        int contentHeight = height - PADDING * 2 - BORDER * 2;

        Element icon = tooltip.getIcon();
        int rowX = innerX;
        if (icon != null) {
            icon.alpha = alpha;
            icon.x = innerX;
            icon.y = innerY + Math.max(0, contentHeight - icon.height) / 2;
            icon.extractRenderState(graphics, -1, -1, 0);
            rowX = innerX + icon.width + ICON_GAP;
        }

        int rowY = innerY;
        for (Tooltip.Line line : tooltip.lines()) {
            int lineHeight = 0;
            for (Element element : line.elements()) {
                lineHeight = Math.max(lineHeight, element.height);
            }
            int elementX = rowX;
            for (Element element : line.elements()) {
                element.alpha = alpha;
                element.x = elementX;
                element.y = rowY;
                element.extractRenderState(graphics, -1, -1, 0);
                elementX += element.width + ELEMENT_GAP;
            }
            rowY += lineHeight + line.marginBottom();
        }
    }
}
