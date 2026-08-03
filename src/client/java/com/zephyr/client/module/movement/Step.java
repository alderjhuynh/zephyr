package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
import com.zephyr.client.configplusgui.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class Step extends Module {
    public static final Step INSTANCE = new Step();

    private final NumberSetting height = new NumberSetting("Height", 1.25D, 0.6D, 3.0D, 0.05D);
    private Double previousHeight;

    private Step() {
        super("Step", "Raises the step height", Category.MOVEMENT);
        addSetting(height);
    }

    @Override
    protected void onEnable() {
        applyHeight(Minecraft.getInstance());
    }

    @Override
    public void tick(Minecraft client) {
        applyHeight(client);
    }

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
