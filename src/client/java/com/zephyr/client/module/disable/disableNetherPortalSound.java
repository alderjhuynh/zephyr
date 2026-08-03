package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class disableNetherPortalSound extends Module {
    public static final disableNetherPortalSound INSTANCE = new disableNetherPortalSound();
    private disableNetherPortalSound() { super("Disable Portal Sound", "Mutes nether portal ambience", Category.DISABLE); }
}
