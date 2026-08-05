package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class disableFogRendering extends Module {
    public static final disableFogRendering INSTANCE = new disableFogRendering();
    private disableFogRendering() { super("Disable Fog", "Hides fog rendering", Category.DISABLE); }
}
