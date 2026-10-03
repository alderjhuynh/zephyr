package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;

public final class IceSpeed extends Module {
    public static final IceSpeed INSTANCE = new IceSpeed();

    private final NumberSetting friction = new NumberSetting("Friction", 0.6D, 0.1D, 1.0D, 0.1D);

    private IceSpeed() {
        super("Ice Speed", "Stops you from sliding uncontrollably on ice", Category.MOVEMENT);
        addSetting(friction);
    }

    public float friction() {
        return friction.get().floatValue();
    }
}
