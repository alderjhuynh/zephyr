package com.zephyr.client.module.qol.shulkerboxtooltip;

import net.minecraft.world.item.DyeColor;

/**
 * The colors used for the preview window background, derived from the shulker box dye
 * color (same defaults as the original ShulkerBoxTooltip mod).
 */
public enum ColorKey {
    DEFAULT(0xffffff, null),
    SHULKER_BOX(0x976797, null),
    WHITE_SHULKER_BOX(0xffffff, DyeColor.WHITE),
    ORANGE_SHULKER_BOX(0xffffff, DyeColor.ORANGE),
    MAGENTA_SHULKER_BOX(0xffffff, DyeColor.MAGENTA),
    LIGHT_BLUE_SHULKER_BOX(0xffffff, DyeColor.LIGHT_BLUE),
    YELLOW_SHULKER_BOX(0xffffff, DyeColor.YELLOW),
    LIME_SHULKER_BOX(0xffffff, DyeColor.LIME),
    PINK_SHULKER_BOX(0xffffff, DyeColor.PINK),
    GRAY_SHULKER_BOX(0xffffff, DyeColor.GRAY),
    LIGHT_GRAY_SHULKER_BOX(0xffffff, DyeColor.LIGHT_GRAY),
    CYAN_SHULKER_BOX(0xffffff, DyeColor.CYAN),
    PURPLE_SHULKER_BOX(0xffffff, DyeColor.PURPLE),
    BLUE_SHULKER_BOX(0xffffff, DyeColor.BLUE),
    BROWN_SHULKER_BOX(0xffffff, DyeColor.BROWN),
    GREEN_SHULKER_BOX(0xffffff, DyeColor.GREEN),
    RED_SHULKER_BOX(0xffffff, DyeColor.RED),
    BLACK_SHULKER_BOX(0xffffff, DyeColor.BLACK);

    private final int rgb;

    /**
     * Builds the background RGB from the dye's diffuse color, dimming it to at
     * least 15% brightness and falling back to {@code fallbackRgb} when no dye is
     * associated.
     */
    ColorKey(int fallbackRgb, DyeColor dye) {
        if (dye == null) {
            this.rgb = fallbackRgb;
        } else {
            int color = dye.getTextureDiffuseColor();
            float r = Math.max(0.15f, ((color >> 16) & 0xff) / 255f);
            float g = Math.max(0.15f, ((color >> 8) & 0xff) / 255f);
            float b = Math.max(0.15f, (color & 0xff) / 255f);
            this.rgb = (toByte(r) << 16) | (toByte(g) << 8) | toByte(b);
        }
    }

    /** @return the preview background color as a packed RGB value */
    public int rgb() {
        return rgb;
    }

    private static int toByte(float v) {
        return Math.round(v * 255f);
    }
}
