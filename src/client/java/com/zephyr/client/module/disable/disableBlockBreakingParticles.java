package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class disableBlockBreakingParticles extends Module {
    public static final disableBlockBreakingParticles INSTANCE = new disableBlockBreakingParticles();
    private disableBlockBreakingParticles() { super("Disable Block Particles", "Hides block breaking particles", Category.DISABLE); }
}
