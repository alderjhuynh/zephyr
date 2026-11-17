package com.zephyr.client.configplusgui.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Groups modules for the click-gui's category filter and per-row tag label.
 * <p>
 * The built-in buckets mirror the {@code module}/{@code mixin} sub-packages
 * (movement, combat, qol, disable, bot) plus two general-purpose ones. Addons can
 * add their own categories at runtime via {@link #of(String)} (exposed as
 * {@code ZephyrApi.registerCategory}), and they show up as extra rows in the
 * click-gui's category dropdown. Every category is a singleton instance, so
 * identity comparison ({@code ==}) and equals/hashCode work as expected.
 */
public final class Category {
    /** Registry of every category, in creation order (built-ins first, addons after). */
    private static final List<Category> VALUES = new ArrayList<>();

    public static final Category MOVEMENT = register("Movement");
    public static final Category DISABLE = register("Disable");
    public static final Category QOL = register("QoL");
    public static final Category COMBAT = register("Combat");
    public static final Category BOT = register("Bot");
    public static final Category HIDDEN = register("Hidden");

    private final String displayName;

    private Category(String displayName) {
        this.displayName = displayName;
    }

    /** Creates a singleton category and adds it to the registry; used for the built-ins. */
    private static Category register(String displayName) {
        Category category = new Category(displayName);
        VALUES.add(category);
        return category;
    }

    /**
     * Returns the category with the given display name, creating and registering it
     * if it doesn't exist yet. Matching is case-insensitive, so addons can safely
     * call this repeatedly for the same name.
     *
     * @param displayName the human-readable label shown in the click-gui
     * @return the existing or newly created category
     */
    public static Category of(String displayName) {
        if (displayName != null) {
            for (Category category : VALUES) {
                if (category.displayName.equalsIgnoreCase(displayName)) {
                    return category;
                }
            }
        }
        Category category = new Category(displayName == null ? "Uncategorized" : displayName);
        VALUES.add(category);
        return category;
    }

    /** Every registered category in registration order, read-only. */
    public static List<Category> all() {
        return Collections.unmodifiableList(new ArrayList<>(VALUES));
    }

    /** Every registered category in registration order (array form, like the old enum's {@code values()}). */
    public static Category[] values() {
        return VALUES.toArray(new Category[0]);
    }

    /** The human-readable label shown on the click-gui tab and module rows. */
    public String getDisplayName() {
        return displayName;
    }
}
