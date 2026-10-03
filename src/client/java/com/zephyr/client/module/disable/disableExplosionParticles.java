package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

public final class disableExplosionParticles extends Module {
    public static final disableExplosionParticles INSTANCE = new disableExplosionParticles();
    private disableExplosionParticles() { super("Disable Explosion Particles", "Hides explosion, explosion emitter, poof, and smoke particles", Category.DISABLE); }
}
