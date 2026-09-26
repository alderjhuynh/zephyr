package com.zephyr.client.module.qol.seedcracker.cracker;

import com.seedfinding.mcbiome.biome.Biome;
import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mccore.version.MCVersion;
import com.zephyr.client.module.qol.seedcracker.config.Config;

/**
 * A single observed biome constraint used to filter candidate world seeds.
 *
 * <p>Records the biome at a given block coordinate sampled from a newly generated chunk. During
 * the biome search phase these constraints are tested against {@link BiomeSource} instances so
 * only seeds whose biomes match the observed layout survive.
 */
public class BiomeData {

    /** The seedfinding biome observed at {@link #x}, {@link #z}. */
    public final Biome biome;
    /** X coordinate of the sample (block or biome coordinate depending on version). */
    public final int x;
    /** Z coordinate of the sample (block or biome coordinate depending on version). */
    public final int z;

    public BiomeData(Biome biome, int x, int z) {
        this.biome = biome;
        this.x = x;
        this.z = z;
    }

    /**
     * Checks whether the given biome source produces this constraint's biome at its position.
     *
     * @param source the biome source to test
     * @return true if the observed biome matches at the recorded coordinates
     */
    public boolean test(BiomeSource source) {
        if (Config.get().getVersion().isNewerOrEqualTo(MCVersion.v1_15)) {
            return source.getBiomeForNoiseGen(this.x, 0, this.z) == this.biome;
        } else {
            return source.getBiome(this.x, 0, this.z) == this.biome;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BiomeData)) return false;
        BiomeData data = (BiomeData) o;
        return this.biome == data.biome;
    }

    @Override
    public int hashCode() {
        return this.biome.getName().hashCode();
    }
}
