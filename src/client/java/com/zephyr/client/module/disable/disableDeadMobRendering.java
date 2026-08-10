package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that hides mobs which are in the death animation state.
 * A simple toggle with no settings; it is backed by {@code DeadMobRenderingMixin},
 * which cancels {@code LivingEntityRenderer#submit} for entities with a positive
 * death time while enabled.
 */
public final class disableDeadMobRendering extends Module {
    public static final disableDeadMobRendering INSTANCE = new disableDeadMobRendering();
    private disableDeadMobRendering() { super("Disable Dead Mob Rendering", "Hides dead mobs", Category.DISABLE); }
}
