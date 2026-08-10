package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that blocks interactions with already-dead mobs. A simple
 * toggle with no settings and no dedicated mixin; the "Disable Dead Mob Interaction"
 * behaviour is exposed for configuration only.
 */
public final class disableDeadMobInteraction extends Module {
    public static final disableDeadMobInteraction INSTANCE = new disableDeadMobInteraction();
    private disableDeadMobInteraction() { super("Disable Dead Mob Interaction", "Blocks interactions with dead mobs", Category.DISABLE); }
}
