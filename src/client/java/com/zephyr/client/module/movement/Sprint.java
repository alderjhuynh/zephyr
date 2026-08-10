package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;

/**
 * Movement module that automatically sets the sprinting state every tick while
 * the module is enabled (except when in water), so the player always moves at
 * sprint speed.
 */
public final class Sprint extends Module {
    public static final Sprint INSTANCE = new Sprint();

    private Sprint() {
        super("Sprint", "Automatically sprints while moving", Category.MOVEMENT);
    }

    /**
     * Forces the sprinting flag on each tick, skipping players in water.
     *
     * @param client the Minecraft client instance
     */
    @Override
    public void tick(Minecraft client) {
        if (client.player != null && !client.player.isInWater()) {
            client.player.setSprinting(true);
        }
    }
}
