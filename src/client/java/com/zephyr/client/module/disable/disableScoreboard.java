package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that hides the sidebar scoreboard from the HUD. A simple
 * toggle with no settings; it is backed by {@code HudMixin}, which cancels the HUD's
 * {@code extractScoreboardSidebar} rendering while enabled.
 */
public final class disableScoreboard extends Module {
    public static final disableScoreboard INSTANCE = new disableScoreboard();
    private disableScoreboard() { super("Disable Scoreboard", "Hides the sidebar scoreboard", Category.DISABLE); }
}
