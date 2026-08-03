package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
import com.zephyr.client.configplusgui.NumberSetting;

public final class HighJump extends Module {
    public static final HighJump INSTANCE = new HighJump();

    private final NumberSetting multiplier = new NumberSetting("Multiplier", 1.5D, 1.0D, 5.0D, 0.1D);

    private HighJump() {
        super("High Jump", "Increases jump height", Category.MOVEMENT);
        addSetting(multiplier);
    }

    public static float modifyJumpVelocity(float velocity) {
        return INSTANCE.isEnabled() ? (float) (velocity * INSTANCE.multiplier.get()) : velocity;
    }
}
