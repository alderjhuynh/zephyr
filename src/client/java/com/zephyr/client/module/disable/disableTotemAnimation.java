package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that prevents the totem of undying pop-up animation and its
 * effects from playing. A simple toggle with no settings; it is backed by
 * {@code ClientPacketListenerMixin}, which cancels entity event packets with the totem
 * event id ({@code 35}) while enabled.
 */
public final class disableTotemAnimation extends Module {
    public static final disableTotemAnimation INSTANCE = new disableTotemAnimation();
    private disableTotemAnimation() { super("Disable Totem Animation", "Prevents the totem of undying pop-up animation and effects", Category.DISABLE); }
}
