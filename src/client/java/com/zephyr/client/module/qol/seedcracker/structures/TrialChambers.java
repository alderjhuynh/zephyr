package com.zephyr.client.module.qol.seedcracker.structures;

import com.seedfinding.mcbiome.biome.Biome;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mccore.version.VersionMap;
import com.seedfinding.mcfeature.structure.RegionStructure;
import com.seedfinding.mcfeature.structure.UniformStructure;

/**
 * Seedfinding model for the trial chambers structure (1.21+).
 *
 * <p>Extends {@link UniformStructure} so trial chambers participate in the standard uniform
 * region-based seed reduction.
 */
public class TrialChambers extends UniformStructure<TrialChambers> {

    public static final VersionMap<Config> CONFIGS = new VersionMap<Config>()
            .add(MCVersion.v1_21, new Config(34, 12, 94251327));

    public TrialChambers(MCVersion version) {
        this(CONFIGS.getAsOf(version), version);
    }

    public TrialChambers(RegionStructure.Config config, MCVersion version) {
        super(config, version);
    }

    /**
     * @return the structure id, "trial_chambers"
     */
    public static String name() {
        return "trial_chambers";
    }

    /**
     * @return the dimension this structure generates in
     */
    @Override
    public Dimension getValidDimension() {
        return Dimension.OVERWORLD;
    }

    /**
     * @param biome the biome to validate
     * @return always true until the deep dark biome is modelled
     */
    @Override
    public boolean isValidBiome(Biome biome) {
        // FIXME: Deep Dark doesn't exist
        return true;
    }
}