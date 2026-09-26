package com.zephyr.client.module.hidden;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Hidden module only shown when {@link com.zephyr.client.configplusgui.module.HiddenModules#shouldShowHidden()} is true
 * (local player's UUID is 1176ac92-5aec-4761-96ab-6ead488f8bb5 or TestMode is true).
 * No actual logic.
 */
public final class WhatEvenIsThis extends Module {
    public static final WhatEvenIsThis INSTANCE = new WhatEvenIsThis();

    private WhatEvenIsThis() {
        super("what even is this", "???", Category.HIDDEN, false, true);
    }
}
