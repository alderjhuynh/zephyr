package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.ListSetting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Locale;

/**
 * Disable-category module that hides rendering for any particle type named in
 * its list setting. Each list row's block-name field holds a particle id
 * (e.g. {@code smoke} or {@code minecraft:smoke}); the color field is ignored.
 * Backed by {@code ParticlesMixin}, which cancels {@code ClientLevel#addParticle}
 * for listed types while enabled.
 */
public final class disableParticles extends Module {
    public static final disableParticles INSTANCE = new disableParticles();

    private final ListSetting particles = new ListSetting("Particles");

    private disableParticles() {
        super("Disable Particles", "Hides rendering for all particles in the list", Category.DISABLE);
        addSetting(particles);
    }

    /** The editable list of particle ids to hide. */
    public ListSetting getParticles() {
        return particles;
    }

    /**
     * Returns true when the given particle's registry id matches an entry in the list.
     * Entries accept either {@code path} ({@code smoke}) or a full id
     * ({@code minecraft:smoke}); bare names default to the {@code minecraft} namespace.
     */
    public boolean shouldDisable(ParticleOptions options) {
        if (options == null || options.getType() == null) return false;
        if (particles.get().isEmpty()) return false;
        var key = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        if (key == null) return false;
        String fullId = key.toString().toLowerCase(Locale.ROOT);
        for (ListSetting.ListEntry entry : particles.get()) {
            String raw = entry.blockName();
            if (raw == null) continue;
            String name = raw.trim().toLowerCase(Locale.ROOT);
            if (name.isEmpty()) continue;
            String normalized = name.contains(":") ? name : "minecraft:" + name;
            if (normalized.equals(fullId)) return true;
        }
        return false;
    }
}
