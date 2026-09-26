package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;

/**
 * Movement module that scales the player's jump velocity, increasing jump
 * height. A living-entity jump mixin routes the vanilla jump velocity through
 * {@link #modifyJumpVelocity}. The multiplier is configurable via the
 * {@code Multiplier} setting (1.0x to 5.0x).
 */
public final class HighJump extends Module {
    public static final HighJump INSTANCE = new HighJump();

    private final NumberSetting multiplier = new NumberSetting("Multiplier", 1.5D, 1.0D, 5.0D, 0.1D);

    private HighJump() {
        super("High Jump", "Increases jump height", Category.MOVEMENT);
        addSetting(multiplier);
    }

    /**
     * Returns the jump velocity scaled by the configured multiplier when the
     * module is enabled, otherwise the velocity is unchanged.
     *
     * @param velocity the vanilla jump velocity
     * @return the effective jump velocity
     */
    public static float modifyJumpVelocity(float velocity) {
        return INSTANCE.isEnabled() ? (float) (velocity * INSTANCE.multiplier.get()) : velocity;
    }
}
