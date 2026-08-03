package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
import com.zephyr.client.configplusgui.NumberSetting;
import net.minecraft.client.Minecraft;

public final class Reach extends Module {
    public static final Reach INSTANCE = new Reach();

    public final NumberSetting blockReach = new NumberSetting("Block Reach", 1D, 0D, 10.0D, 1D);
    public final NumberSetting entityReach = new NumberSetting("Entity Reach", 1D, 0D, 4.0D, 1D);

    private Reach() {
        super("Reach", "Increases reach distance", Category.COMBAT);
        addSetting(blockReach);
        addSetting(entityReach);
    }
}
