package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that keeps GUIs open while passing through nether portals,
 * preventing the automatic screen close on the portal transition effect. A simple
 * toggle with no settings; it is backed by {@code LocalPlayerMixin}, which nulls the
 * current screen during {@code LocalPlayer#handlePortalTransitionEffect} while enabled.
 */
public final class disablePortalGuiClosing extends Module {
    public static final disablePortalGuiClosing INSTANCE = new disablePortalGuiClosing();
    private disablePortalGuiClosing() { super("Disable Portal GUI Closing", "Keeps GUIs open in portals", Category.DISABLE); }
}
