package com.zephyr.client;

public class HighJump {

    public static boolean enabled = false;

    private static double multiplier = 2.0;

    private static final double MIN_MULTIPLIER = 1.0;
    private static final double MAX_MULTIPLIER = 5.0;

    public static float modifyJumpVelocity(float original) {
        if (!enabled) return original;
        return (float) (original * multiplier);
    }

    public static double getMultiplier() {
        return multiplier;
    }

    public static void setMultiplier(double value) {
        multiplier = clamp(value, MIN_MULTIPLIER, MAX_MULTIPLIER);
        ZephyrConfig.saveCurrentState();
    }

    public static double getMinMultiplier() {
        return MIN_MULTIPLIER;
    }

    public static double getMaxMultiplier() {
        return MAX_MULTIPLIER;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
