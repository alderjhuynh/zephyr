package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class disableDeadMobRendering extends Module {
    public static final disableDeadMobRendering INSTANCE = new disableDeadMobRendering();
    private disableDeadMobRendering() { super("Disable Dead Mob Rendering", "Hides dead mobs", Category.DISABLE); }
}
