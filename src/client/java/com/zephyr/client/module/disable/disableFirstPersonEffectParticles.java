package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that hides the ambient status-effect particles (such as
 * poison or regeneration) spawned around the local player while playing in first
 * person. A simple toggle with no settings; it is backed by
 * {@code LivingEntityRendererMixin}, which redirects {@code LivingEntity#tickEffects}
 * particle spawning while enabled.
 */
public final class disableFirstPersonEffectParticles extends Module {
    public static final disableFirstPersonEffectParticles INSTANCE = new disableFirstPersonEffectParticles();
    private disableFirstPersonEffectParticles() { super("Disable First-Person Particles", "Hides own status particles", Category.DISABLE); }
}
