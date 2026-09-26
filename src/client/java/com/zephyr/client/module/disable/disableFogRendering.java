package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that hides all fog rendering (weather, biome, and similar
 * atmospheric fog). A simple toggle with no settings; it is backed by
 * {@code BackgroundRendererMixin}, which substitutes an empty fog uniform buffer in
 * {@code FogRenderer#getBuffer} while enabled.
 */
public final class disableFogRendering extends Module {
    public static final disableFogRendering INSTANCE = new disableFogRendering();
    private disableFogRendering() { super("Disable Fog", "Hides fog rendering", Category.DISABLE); }
}
