package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class disableNauseaOverlay extends Module {
    public static final disableNauseaOverlay INSTANCE = new disableNauseaOverlay();
    private disableNauseaOverlay() { super("Disable Nausea", "Hides nausea overlays when distortion effects are 0", Category.DISABLE); }
}
