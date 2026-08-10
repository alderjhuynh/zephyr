package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;

public final class Sprint extends Module {
    public static final Sprint INSTANCE = new Sprint();

    private Sprint() {
        super("Sprint", "Automatically sprints while moving", Category.MOVEMENT);
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player != null && !client.player.isInWater()) {
            client.player.setSprinting(true);
        }
    }
}
