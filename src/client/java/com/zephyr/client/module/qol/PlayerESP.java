package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class PlayerESP extends Module {
    public static final PlayerESP INSTANCE = new PlayerESP();
    private PlayerESP() {
        super("PlayerESP", "Glows nearby players", Category.QOL);
    }
}
