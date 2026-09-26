package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Movement module that raises the player's step height so taller blocks can be
 * climbed automatically. It stores the original {@link Attributes#STEP_HEIGHT}
 * base value on first enable and restores it on disable. The height is
 * configurable via the {@code Height} setting (0.6 to 3.0 blocks).
 */
public final class Step extends Module {
    public static final Step INSTANCE = new Step();

    private final NumberSetting height = new NumberSetting("Height", 1.25D, 0.6D, 3.0D, 0.05D);
    private Double previousHeight;

    private Step() {
        super("Step", "Raises the step height", Category.MOVEMENT);
        addSetting(height);
    }

    /** Applies the configured step height when the module is enabled. */
    @Override
    protected void onEnable() {
        applyHeight(Minecraft.getInstance());
    }

    /** Keeps the step-height attribute in sync with the setting each tick. */
    @Override
    public void tick(Minecraft client) {
        applyHeight(client);
    }

    /** Restores the original step height when the module is disabled. */
    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || previousHeight == null) {
            return;
        }

        AttributeInstance attribute = client.player.getAttribute(Attributes.STEP_HEIGHT);
        if (attribute != null) {
            attribute.setBaseValue(previousHeight);
        }
        previousHeight = null;
    }

    private void applyHeight(Minecraft client) {
        if (client.player == null) {
            return;
        }

        AttributeInstance attribute = client.player.getAttribute(Attributes.STEP_HEIGHT);
        if (attribute == null) {
            return;
        }

        if (previousHeight == null) {
            previousHeight = attribute.getBaseValue();
        }
        attribute.setBaseValue(height.get());
    }
}
