package com.zephyr.client.configplusgui;

/**
 * Groups modules for the click-gui's tab bar and per-row tag label. Mirrors the
 * {@code module}/{@code mixin} sub-packages (movement, combat, qol, disable) plus
 * two general-purpose buckets.
 */
public enum Category {
    MOVEMENT("Movement"),
    DISABLE("Disable"),
    QOL("QoL"),
    COMBAT("Combat");



    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
