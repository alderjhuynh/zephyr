package com.zephyr.client.configplusgui.setting;

/**
 * A {@link Setting} holding a bounded double value with a fixed step, rendered as a
 * draggable slider in the click-gui. Values are always clamped to [min, max] and snapped
 * to the step on both construction and assignment.
 */
public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    /**
     * Creates a numeric setting; the default is snapped and clamped into range.
     *
     * @param step the increment for snapping; values {@code <= 0} fall back to 0.01
     */
    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        super(name, clamp(snap(defaultValue, min, step), min, max));
        this.min = min;
        this.max = max;
        this.step = step > 0 ? step : 0.01D;
    }

    /** The lower bound of the slider range. */
    public double getMin() {
        return min;
    }

    /** The upper bound of the slider range. */
    public double getMax() {
        return max;
    }

    /** The snapping increment used by {@link #set(double)}. */
    public double getStep() {
        return step;
    }

    /** Sets the value, clamping it into [min, max] and snapping it to the step. */
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

    /** Rounds {@code value} to the nearest multiple of {@code step} above {@code min}. */
    private static double snap(double value, double min, double step) {
        if (step <= 0) return value;
        return min + Math.round((value - min) / step) * step;
    }

    /** Clamps {@code value} into [min, max]. */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
