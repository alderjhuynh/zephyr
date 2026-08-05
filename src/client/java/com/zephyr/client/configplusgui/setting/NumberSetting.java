package com.zephyr.client.configplusgui.setting;

/**
 * Renders as a draggable slider in the click-gui, bounded to [min, max] and
 * snapped to {@code step}. Use {@link #getProgress()} / {@link #setFromProgress(double)}
 * to convert to/from a 0-1 fraction of the slider's width.
 */
public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        super(name, clamp(snap(defaultValue, min, step), min, max));
        this.min = min;
        this.max = max;
        this.step = step > 0 ? step : 0.01D;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public void set(double value) {
        setValue(clamp(snap(value, min, step), min, max));
    }

    /** Current value as a fraction of the way between min and max, for drawing the slider fill. */
    public double getProgress() {
        if (max <= min) return 0D;
        return (get() - min) / (max - min);
    }

    /** Sets the value from a 0-1 fraction, typically derived from mouse position over the slider track. */
    public void setFromProgress(double progress) {
        double clampedProgress = Math.max(0D, Math.min(1D, progress));
        set(min + clampedProgress * (max - min));
    }

    private static double snap(double value, double min, double step) {
        if (step <= 0) return value;
        return min + Math.round((value - min) / step) * step;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
