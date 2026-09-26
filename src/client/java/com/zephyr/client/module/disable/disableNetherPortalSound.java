package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that mutes the looping nether portal ambience
 * ({@code block.portal.ambient}). A simple toggle with no settings; it is backed by
 * {@code SoundManagerMixin}, which prevents the portal sound from being played while
 * enabled.
 */
public final class disableNetherPortalSound extends Module {
    public static final disableNetherPortalSound INSTANCE = new disableNetherPortalSound();
    private disableNetherPortalSound() { super("Disable Portal Sound", "Mutes nether portal ambience", Category.DISABLE); }
}
