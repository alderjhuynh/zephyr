package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class disableDeadMobInteraction extends Module {
    public static final disableDeadMobInteraction INSTANCE = new disableDeadMobInteraction();
    private disableDeadMobInteraction() { super("Disable Dead Mob Interaction", "Blocks interactions with dead mobs", Category.DISABLE); }
}
