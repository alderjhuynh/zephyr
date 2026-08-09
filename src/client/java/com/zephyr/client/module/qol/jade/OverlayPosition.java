package com.zephyr.client.module.qol.jade;

/**
 * Where the Jade tooltip is anchored on the screen. Mirrors Jade's configurable
 * overlay position, but simplified to a handful of screen-anchor presets plus
 * the {@link Jade} module's X/Y offset settings for fine tuning.
 */
public enum OverlayPosition {
    TOP_LEFT("Top Left"),
    TOP_CENTER("Top Center"),
    TOP_RIGHT("Top Right"),
    CENTER("Center"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_CENTER("Bottom Center"),
    BOTTOM_RIGHT("Bottom Right");

    private final String displayName;

    OverlayPosition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
