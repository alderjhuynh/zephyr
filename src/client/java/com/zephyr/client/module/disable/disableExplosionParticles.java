package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that hides explosion particles: the explosion,
 * explosion emitter, poof, and smoke particles. A simple toggle with no settings; it is
 * backed by {@code ExplosionParticleMixin}, which cancels
 * {@code ClientLevel#addParticle} for those particle types while enabled.
 */
public final class disableExplosionParticles extends Module {
    public static final disableExplosionParticles INSTANCE = new disableExplosionParticles();
    private disableExplosionParticles() { super("Disable Explosion Particles", "Hides explosion, explosion emitter, poof, and smoke particles", Category.DISABLE); }
}
