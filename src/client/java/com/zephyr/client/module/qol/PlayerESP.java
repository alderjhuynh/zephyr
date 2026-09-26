package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Makes nearby players glow so they are easy to spot through walls. The glow
 * effect is applied by the associated mixin (via the glowing team effect) while
 * this module is enabled; the module itself carries no per-tick logic.
 */
public final class PlayerESP extends Module {
    public static final PlayerESP INSTANCE = new PlayerESP();
    private PlayerESP() {
        super("PlayerESP", "Glows nearby players", Category.QOL);
    }
}
