package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Forces the client gamma value to its maximum so the world is fully lit even
 * in total darkness. No per-tick logic is needed; the gamma is applied by the
 * associated setting sync mixin while the module is enabled.
 */
public final class FullBright extends Module {
    public static final FullBright INSTANCE = new FullBright();
    private FullBright() {
        super("FullBright", "Increases gamma", Category.QOL);
    }
}
