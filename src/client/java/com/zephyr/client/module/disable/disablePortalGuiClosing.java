package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class disablePortalGuiClosing extends Module {
    public static final disablePortalGuiClosing INSTANCE = new disablePortalGuiClosing();
    private disablePortalGuiClosing() { super("Disable Portal GUI Closing", "Keeps GUIs open in portals", Category.DISABLE); }
}
