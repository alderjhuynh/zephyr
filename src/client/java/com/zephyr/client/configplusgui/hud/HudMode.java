package com.zephyr.client.configplusgui.hud;

/**
 * The three levels of the in-game HUD overlay controlled by
 * {@link GlobalConfig#hudMode()}. {@link HudRenderer} renders nothing for {@code OFF}, a
 * small top-left watermark/profile panel for {@code MINIMAL}, and adds the enabled-modules
 * list (top-right) plus a bottom-left info stack (FPS, server, module count, profile,
 * theme) for {@code FULL}.
 */
public enum HudMode {
    OFF,
    MINIMAL,
    FULL;
}
