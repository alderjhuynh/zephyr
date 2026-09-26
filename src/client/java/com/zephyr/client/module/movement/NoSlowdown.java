package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Movement module that cancels movement speed reductions while the module is
 * enabled. It is a marker module: the actual behavior is implemented by the
 * {@code ItemUseMixin}, {@code WaterMixin} and {@code WebMixin} mixins, which
 * skip the vanilla slowdown effects when this module's {@link #INSTANCE} is
 * enabled. Covered sources of slowdown are using items (e.g. eating or
 * blocking), walking through water, and moving inside cobwebs.
 */
public final class NoSlowdown extends Module {
    public static final NoSlowdown INSTANCE = new NoSlowdown();

    private NoSlowdown() {
        super("No Slowdown", "Cancels the movement speed reduction from using items, walking in webs, or pushing through water",
                Category.MOVEMENT);
    }
}
