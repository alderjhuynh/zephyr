package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Allows the player to move while a GUI is open. The movement input processing
 * is unblocked by the associated mixin whenever this module is enabled, so the
 * module itself carries no per-tick logic.
 */
public final class GuiMove extends Module {
    public static final GuiMove INSTANCE = new GuiMove();
    private GuiMove() {
        super("Gui Move", "Allows movement inputs while GUIs are open", Category.QOL);
    }

}
