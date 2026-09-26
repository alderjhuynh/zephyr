package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that hides boss health bars. A simple toggle with no
 * settings; it is backed by {@code HudMixin}, which cancels the HUD's
 * {@code extractBossOverlay} rendering while enabled.
 */
public final class disableBossbar extends Module {
    public static final disableBossbar INSTANCE = new disableBossbar();
    private disableBossbar() { super("Disable Bossbar", "Hides bossbars", Category.DISABLE); }
}
