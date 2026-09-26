package com.zephyr.client.configplusgui.hud;

/**
 * The six preset accent color schemes for the Zephyr UI. Each carries an RGB base color
 * from which {@link GlobalConfig} derives the fully opaque accent, a translucent dim accent
 * (e.g. click-gui scroll bar) and a faint background wash for enabled rows. The active
 * preset is persisted in the client config and can be overridden by the custom HSV color.
 */
public enum ThemeColor {
    LAVENDER(0xD1B9EB),
    SKY(0x8FC7F0),
    MINT(0x8FE3C0),
    GOLD(0xF0D58F),
    CORAL(0xF0A08F),
    ROSE(0xF08FC9);

    private final int rgb;

    ThemeColor(int rgb) {
        this.rgb = rgb;
    }

    /** The fully opaque accent color. */
    public int accent() {
        return 0xFF000000 | rgb;
    }

    /** A partially transparent accent variant for subtle UI elements. */
    public int accentDim() {
        return 0x66000000 | rgb;
    }

    /** A faint accent wash behind enabled/highlighted rows. */
    public int enabledBg() {
        return 0x40000000 | rgb;
    }

    /** The preset's display name, e.g. {@code LAVENDER -> "Lavender"}. */
    public String displayName() {
        String name = name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
