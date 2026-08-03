package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class disableRainEffects extends Module {
    public static final disableRainEffects INSTANCE = new disableRainEffects();
    private disableRainEffects() { super("Disable Rain", "Hides rain and rain sounds", Category.DISABLE); }
}
