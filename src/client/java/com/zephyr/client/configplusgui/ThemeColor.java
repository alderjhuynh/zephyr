package com.zephyr.client.configplusgui;

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

    public int accent() {
        return 0xFF000000 | rgb;
    }

    public int accentDim() {
        return 0x66000000 | rgb;
    }

    public int enabledBg() {
        return 0x40000000 | rgb;
    }

    public String displayName() {
        String name = name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
