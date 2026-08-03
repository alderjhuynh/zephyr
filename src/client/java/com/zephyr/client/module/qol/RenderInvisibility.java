package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class RenderInvisibility extends Module {
    public static final RenderInvisibility INSTANCE = new RenderInvisibility();
    private RenderInvisibility() {
        super("Render Invisibility", "Renders invisible players as translucent", Category.QOL);
    }
}
