package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that hides the nausea screen distortion and vignette
 * overlays when the distortion effect strength is zero. A simple toggle with no
 * settings; it is backed by {@code NauseaOverlayMixin} and {@code NauseaVignetteMixin}
 * which suppress the wobble and the confusion overlay while enabled.
 */
public final class disableNauseaOverlay extends Module {
    public static final disableNauseaOverlay INSTANCE = new disableNauseaOverlay();
    private disableNauseaOverlay() { super("Disable Nausea", "Hides nausea overlays when distortion effects are 0", Category.DISABLE); }
}
