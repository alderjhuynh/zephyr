package com.zephyr.client.module.qol.shulkerboxtooltip.render;

import com.zephyr.client.module.qol.shulkerboxtooltip.ColorKey;
import com.zephyr.client.module.qol.shulkerboxtooltip.PreviewType;
import com.zephyr.client.module.qol.shulkerboxtooltip.util.ShulkerBoxTooltipUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The ShulkerBoxTooltip window renderer: a colored, tinted nine-slice window with 18x18 slots.
 */
public class ModPreviewRenderer extends BasePreviewRenderer {
    public static final ModPreviewRenderer INSTANCE = new ModPreviewRenderer();

    private static final Identifier DEFAULT_TEXTURE_LIGHT = ShulkerBoxTooltipUtil.id("shulker_box_tooltip");
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace(
            "container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace(
            "container/slot_highlight_front");

    /** Package-private singleton constructor using 18x18 slots with 8px padding. */
    ModPreviewRenderer() {
        super(18, 18, 8, 8);
    }

    /** @return the pixel width of the rendered window */
    @Override
    public int getWidth() {
        return 14 + Math.min(this.getMaxRowSize(), this.getInvSize()) * 18;
    }

    /** @return the pixel height of the rendered window */
    @Override
    public int getHeight() {
        return 14 + (int) Math.ceil(this.getInvSize() / (double) this.getMaxRowSize()) * 18;
    }

    /** @return the number of slots shown by the current preview type */
    private int getInvSize() {
        return this.previewType == PreviewType.COMPACT ?
                Math.max(1, this.compactItems.size()) :
                this.provider.getInventoryMaxSize(this.previewContext);
    }

    /** Sets the color of the preview window. */
    private int getColor() {
        ColorKey key;

        if (this.config.useColors()) {
            key = this.provider.getWindowColorKey(this.previewContext);
        } else {
            key = ColorKey.DEFAULT;
        }
        return 0xFF000000 | key.rgb();
    }

    /** @return the texture override, or the default ShulkerBoxTooltip sprite */
    private Identifier getTexture() {
        if (this.textureOverride != null)
            return this.textureOverride;
        return DEFAULT_TEXTURE_LIGHT;
    }

    /** Draws the tinted nine-slice window sized to fit the current preview. */
    private void drawBackground(int x, int y, GuiGraphicsExtractor graphics) {
        int invSize = this.getInvSize();
        int slotSize = 18;
        int padding = 14; // extra space for the GUI borders

        int maxColumns = this.getMaxRowSize();

        int columns = Math.min(maxColumns, invSize);
        int rows = (int) Math.ceil(invSize / (double) maxColumns);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getTexture(), x, y, padding + (columns * slotSize),
                padding + (rows * slotSize), this.getColor());
    }

    /** Draws the window background, slots, and hovered-slot tooltip. */
    @Override
    public void draw(RenderContext context) {
        if (this.compactItems.isEmpty() || this.previewType == PreviewType.NO_PREVIEW)
            return;

        int viewportWidth = context.viewportWidth();
        int x = context.x();
        int y = context.y();
        var graphics = context.graphics();
        var font = context.font();
        int mouseX = context.mouseX();
        int mouseY = context.mouseY();

        this.drawBackground(x, y, graphics);
        if (this.previewType == PreviewType.FULL) {
            int maxSlots = this.previewContext != null ?
                    this.provider.getInventoryMaxSize(this.previewContext) :
                    Integer.MAX_VALUE;
            this.drawSlots(x, y, graphics, font, mouseX, mouseY, maxSlots - 1);
        } else {
            this.drawSlots(x, y, graphics, font, mouseX, mouseY, Integer.MAX_VALUE);
        }
        this.drawInnerTooltip(x, y, graphics, font, mouseX, mouseY);
    }

    /** Draws a single 18x18 slot, highlighting it and rendering its item. */
    @Override
    protected void drawSlot(ItemStack stack, int x, int y, GuiGraphicsExtractor graphics, Font font, int slot,
                            boolean isHighlighted, boolean shortItemCount) {
        int maxRowSize = this.getMaxRowSize();
        int sx = this.slotXOffset + x + this.slotWidth * (slot % maxRowSize);
        int sy = this.slotYOffset + y + this.slotHeight * (slot / maxRowSize);

        if (isHighlighted) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, sx - 4, sy - 4, 24, 24);
        }

        if (!stack.isEmpty())
            this.drawItem(stack, sx, sy, graphics, font, shortItemCount);

        if (isHighlighted) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, sx - 4, sy - 4, 24, 24);
        }
    }
}
