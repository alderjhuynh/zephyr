package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
import net.minecraft.client.Minecraft;

public final class HoldUse extends Module {
    public static final HoldUse INSTANCE = new HoldUse();
    private HoldUse() {
        super("Hold Use", "Continually simulates pressing the use key", Category.QOL);
    }


    @Override
    public void tick(Minecraft client) {
        client.options.keyUse.setDown(true);
    }
}
