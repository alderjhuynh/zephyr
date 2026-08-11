package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that removes the camera tilt applied when the player takes damage.
 * A simple toggle with no settings; it is backed by {@code GameRendererMixin} which cancels
 * {@code GameRenderer.bobHurt} while enabled.
 */
public final class disableDamageTilt extends Module {
    public static final disableDamageTilt INSTANCE = new disableDamageTilt();

    private disableDamageTilt() {
        super("Disable Damage Tilt", "Removes the camera tilt when you take damage", Category.DISABLE);
    }
}
