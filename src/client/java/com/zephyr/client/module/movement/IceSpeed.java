package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;

/**
 * Movement module that removes the slippery behavior of ice blocks. The IceSpeed Block mixin
 * raises the friction of any slippery block the local player stands on to the configured
 * value, so the player stops on ice instead of sliding uncontrollably.
 */
public final class IceSpeed extends Module {
    public static final IceSpeed INSTANCE = new IceSpeed();

    private final NumberSetting friction = new NumberSetting("Friction", 0.6D, 0.1D, 1.0D, 0.1D);

    private IceSpeed() {
        super("Ice Speed", "Stops you from sliding uncontrollably on ice", Category.MOVEMENT);
        addSetting(friction);
    }

    /** The friction value substituted for slippery blocks the player stands on. */
    public float friction() {
        return friction.get().floatValue();
    }
}
