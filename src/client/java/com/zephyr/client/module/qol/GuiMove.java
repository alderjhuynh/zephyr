package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class GuiMove extends Module {
    public static final GuiMove INSTANCE = new GuiMove();
    private GuiMove() {
        super("Gui Move", "Allows movement inputs while GUIs are open", Category.QOL);
    }

}
