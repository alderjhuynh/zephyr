package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Renders invisible players as translucent instead of fully hidden. The
 * translucency is applied by the associated rendering mixin while this module
 * is enabled; the module itself carries no per-tick logic.
 */
public final class RenderInvisibility extends Module {
    public static final RenderInvisibility INSTANCE = new RenderInvisibility();
    private RenderInvisibility() {
        super("Render Invisibility", "Renders invisible players as translucent", Category.QOL);
    }
}
