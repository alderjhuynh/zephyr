package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;

/**
 * Movement module that enables creative-style client flight: while enabled it
 * keeps the flying and fly-permission ability flags on every tick (unless the
 * player is spectating). On disable it restores the flags for players that are
 * not creative or spectating, so flight is not left on for non-creative modes.
 */
public final class Flight extends Module {
    public static final Flight INSTANCE = new Flight();

    private Flight() {
        super("Flight", "Enables client flight", Category.MOVEMENT);
    }

    /**
     * Forces the flying and may-fly ability flags on while the module is active.
     *
     * @param client the Minecraft client instance
     */
    @Override
    public void tick(Minecraft client) {
        if (client.player != null && !client.player.isSpectator()) {
            client.player.getAbilities().flying = true;
            client.player.getAbilities().mayfly = true;
        }
    }

    /** Restores the flight abilities when the module is turned off. */
    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && !client.player.isCreative() && !client.player.isSpectator()) {
            client.player.getAbilities().flying = false;
            client.player.getAbilities().mayfly = false;
        }
    }
}
