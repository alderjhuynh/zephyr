package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
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
