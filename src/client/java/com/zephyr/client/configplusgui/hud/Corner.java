package com.zephyr.client.configplusgui.hud;

/**
 * The four screen corners used to anchor overlay UI elements such as notification toasts
 * (see {@link GlobalConfig#notificationCorner()}). Used by {@link NotificationManager} to
 * decide where a toast stack grows from and which axis it slides along.
 */
public enum Corner {
    TOP_RIGHT,
    TOP_LEFT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT;
}
