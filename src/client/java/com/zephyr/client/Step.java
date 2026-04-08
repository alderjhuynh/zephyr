package com.zephyr.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributes;

public final class Step {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean enabled = false;
    private static double stepHeight = 1.25;

    private static float previousStepHeight = 0.6f; // fallback vanilla default


    public static void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;

        if (mc.player == null) return;

        var attr = mc.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
        if (attr == null) return;

        if (enabled) {
            previousStepHeight = (float) attr.getBaseValue();
            attr.setBaseValue(stepHeight);
        } else {
            attr.setBaseValue(previousStepHeight);
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setStepHeight(double value) {
        stepHeight = value;

        if (!enabled || mc.player == null) return;

        var attr = mc.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
        if (attr != null) {
            attr.setBaseValue(stepHeight);
        }
    }

    public static double getStepHeight() {
        return stepHeight;
    }

    public static void tick() {
        if (!enabled || mc.player == null) return;

        var attr = mc.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
        if (attr != null) {
            attr.setBaseValue(stepHeight);
        }
    }


    public static void reset() {
        if (mc.player == null) return;

        var attr = mc.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
        if (attr != null) {
            attr.setBaseValue(previousStepHeight);
        }

        enabled = false;
    }
}