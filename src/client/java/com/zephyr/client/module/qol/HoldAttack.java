package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
import net.minecraft.client.Minecraft;

public final class HoldAttack extends Module {
    public static final HoldAttack INSTANCE = new HoldAttack();
    private HoldAttack() {
        super("Hold Attack", "Continually simulates pressing the attack key", Category.QOL);
    }


    @Override
    public void tick(Minecraft client) {
        client.options.keyAttack.setDown(true);
    }
}
