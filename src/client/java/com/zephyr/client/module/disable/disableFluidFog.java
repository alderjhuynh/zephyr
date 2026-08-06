package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class disableFluidFog extends Module {
    public static final disableFluidFog INSTANCE = new disableFluidFog();
    private disableFluidFog() { super("Disable Fluid Fog", "Removes fog while underwater or in lava for better visibility", Category.DISABLE); }
}
