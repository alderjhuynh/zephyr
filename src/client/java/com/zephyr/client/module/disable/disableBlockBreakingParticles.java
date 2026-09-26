package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that hides the block breaking particles spawned when a
 * block is destroyed. A simple toggle with no settings; it is backed by
 * {@code BlockParticleMixin}, which cancels {@code ClientLevel#addDestroyBlockEffect}
 * while enabled.
 */
public final class disableBlockBreakingParticles extends Module {
    public static final disableBlockBreakingParticles INSTANCE = new disableBlockBreakingParticles();
    private disableBlockBreakingParticles() { super("Disable Block Particles", "Hides block breaking particles", Category.DISABLE); }
}
