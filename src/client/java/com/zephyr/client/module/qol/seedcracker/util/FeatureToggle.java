package com.zephyr.client.module.qol.seedcracker.util;

/**
 * Mutable boolean wrapper used for feature toggles in the seedcracker config.
 *
 * <p>This is for the featureToggles in the config object. It allows the booleans to be passed
 * around by reference so a shared instance can be updated and read from multiple places.
 */
public class FeatureToggle {

    //This is for The featureToggles in the config object
    //It allows for the booleans to be passed around by reference
    //(I know that it's a hacky workaround)
    private boolean enabled;

    public FeatureToggle(boolean flag) {
        enabled = flag;
    }

    /**
     * @param flag the new enabled state
     */
    public void set(boolean flag) {
        enabled = flag;
    }

    /**
     * @return the current enabled state
     */
    public boolean get() {
        return enabled;
    }
}
