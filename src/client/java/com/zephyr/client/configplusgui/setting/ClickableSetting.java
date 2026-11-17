package com.zephyr.client.configplusgui.setting;

/**
 * Extension point for addon-defined setting types. Any {@link Setting} that is also a
 * {@code ClickableSetting} gets a standard one-row rendering in the click-gui (the
 * setting's name on the left, {@link #getDisplayValue()} right-aligned in accent
 * color) and receives left clicks on that row via {@link #onClick}.
 * <p>
 * Addons subclass this to build fully custom interactive settings without touching
 * Zephyr's GUI code, e.g. a color picker, an action button, or a value cycler. Values
 * still persist normally through {@link ConfigManager} as long as a matching
 * {@code writeSettingValue}/... branch exists - otherwise persist the state yourself.
 */
public abstract class ClickableSetting<T> extends Setting<T> {

    /** Creates a clickable setting with a given name and initial (default) value. */
    protected ClickableSetting(String name, T defaultValue) {
        super(name, defaultValue);
    }

    /**
     * The right-aligned label for the current value; return an empty string to hide it.
     */
    public abstract String getDisplayValue();

    /**
     * Called when the row is left-clicked.
     *
     * @param mouseX the click position in gui-scaled coordinates
     * @param mouseY the click position in gui-scaled coordinates
     * @return {@code true} if the click was consumed (stops further handling)
     */
    public abstract boolean onClick(double mouseX, double mouseY);
}
