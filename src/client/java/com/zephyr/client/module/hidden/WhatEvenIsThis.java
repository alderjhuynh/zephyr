package com.zephyr.client.module.hidden;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class WhatEvenIsThis extends Module {
    public static final WhatEvenIsThis INSTANCE = new WhatEvenIsThis();

    private WhatEvenIsThis() {
        super("what even is this", "???", Category.HIDDEN, false, true);
    }
}
