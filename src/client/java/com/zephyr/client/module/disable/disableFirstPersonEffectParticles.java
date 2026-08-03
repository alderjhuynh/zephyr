package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;

public final class disableFirstPersonEffectParticles extends Module {
    public static final disableFirstPersonEffectParticles INSTANCE = new disableFirstPersonEffectParticles();
    private disableFirstPersonEffectParticles() { super("Disable First-Person Particles", "Hides own status particles", Category.DISABLE); }
}
