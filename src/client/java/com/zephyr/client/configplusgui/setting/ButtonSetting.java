package com.zephyr.client.configplusgui.setting;

/**
 * A {@link Setting} that runs an action when its row is clicked instead of holding a
 * persistent value, e.g. "Open Waypoints Screen" or "Reset Profile". Rendered as a
 * normal settings row with an optional static label on the right. Not persisted.
 */
public final class ButtonSetting extends ClickableSetting<Void> {
    private final Runnable action;
    private final String label;

    /**
     * Creates a button setting.
     *
     * @param name   the row's display name
     * @param label  right-aligned label (e.g. "Click"); empty string hides it
     * @param action the action to run when the row is clicked
     */
    public ButtonSetting(String name, String label, Runnable action) {
        super(name, null);
        this.label = label == null ? "" : label;
        this.action = action;
    }

    /** Creates a button setting with no right-aligned label. */
    public ButtonSetting(String name, Runnable action) {
        this(name, "", action);
    }

    /** The static right-aligned label; empty string means nothing is drawn. */
    @Override
    public String getDisplayValue() {
        return label;
    }

    /** Runs the button's action; always consumes the click. */
    @Override
    public boolean onClick(double mouseX, double mouseY) {
        if (action != null) {
            action.run();
        }
        return true;
    }
}
