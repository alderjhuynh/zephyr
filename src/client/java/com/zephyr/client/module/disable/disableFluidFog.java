package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that removes fog while the player is underwater or in lava
 * for clearer visibility. A simple toggle with no settings; it is backed by both
 * {@code WaterFogEnvironmentMixin} and {@code LavaFogEnvironmentMixin}, which push
 * the fluid fog distances to their maximum values while enabled.
 */
public final class disableFluidFog extends Module {
    public static final disableFluidFog INSTANCE = new disableFluidFog();
    private disableFluidFog() { super("Disable Fluid Fog", "Removes fog while underwater or in lava for better visibility", Category.DISABLE); }
}
