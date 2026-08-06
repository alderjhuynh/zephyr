package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class disableScoreboard extends Module {
    public static final disableScoreboard INSTANCE = new disableScoreboard();
    private disableScoreboard() { super("Disable Scoreboard", "Hides the sidebar scoreboard", Category.DISABLE); }
}
