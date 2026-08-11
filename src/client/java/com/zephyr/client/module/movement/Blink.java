package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Movement module that holds back the player's position and rotation packets while enabled.
 * The server is unaware of movement during this time, so the player appears frozen to others.
 * Once the module is disabled, the queued movement is flushed to the server in the next tick.
 */
public final class Blink extends Module {
    public static final Blink INSTANCE = new Blink();

    private Blink() {
        super("Blink", "Suppresses your position packets while enabled", Category.MOVEMENT);
    }
}
