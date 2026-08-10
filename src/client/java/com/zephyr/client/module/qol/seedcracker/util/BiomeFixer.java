package com.zephyr.client.module.qol.seedcracker.util;

import com.seedfinding.mcbiome.biome.Biome;
import com.seedfinding.mcbiome.biome.Biomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts between vanilla and seedfinding biome objects.
 *
 * <p>Modern biomes that were renamed or added are mapped onto their closest seedfinding equivalent
 * (e.g. "snowy_plains" to snowy tundra) so the cracker can reason about them; unknown cave/deep
 * dark biomes fall back to the void biome.
 */
public class BiomeFixer {

    private static final Map<String, Biome> COMPATREGISTRY = new HashMap<>();

    static {
        for (Biome biome : Biomes.REGISTRY.values()) {
            COMPATREGISTRY.put(biome.getName(), biome);
        }
        //renamed
        COMPATREGISTRY.put("snowy_plains", Biomes.SNOWY_TUNDRA);
        COMPATREGISTRY.put("old_growth_birch_forest", Biomes.TALL_BIRCH_FOREST);
        COMPATREGISTRY.put("old_growth_pine_taiga", Biomes.GIANT_TREE_TAIGA);
        COMPATREGISTRY.put("old_growth_spruce_taiga", Biomes.GIANT_TREE_TAIGA);
        COMPATREGISTRY.put("windswept_hills", Biomes.EXTREME_HILLS);
        COMPATREGISTRY.put("windswept_forest", Biomes.WOODED_MOUNTAINS);
        COMPATREGISTRY.put("windswept_gravelly_hills", Biomes.GRAVELLY_MOUNTAINS);
        COMPATREGISTRY.put("windswept_savanna", Biomes.SHATTERED_SAVANNA);
        COMPATREGISTRY.put("sparse_jungle", Biomes.JUNGLE_EDGE);
        COMPATREGISTRY.put("stony_shore", Biomes.STONE_SHORE);
        //new
        COMPATREGISTRY.put("meadow", Biomes.PLAINS);
        COMPATREGISTRY.put("grove", Biomes.TAIGA);
        COMPATREGISTRY.put("snowy_slopes", Biomes.SNOWY_TUNDRA);
        COMPATREGISTRY.put("frozen_peaks", Biomes.TAIGA);
        COMPATREGISTRY.put("jagged_peaks", Biomes.TAIGA);
        COMPATREGISTRY.put("stony_peaks", Biomes.TAIGA);
        COMPATREGISTRY.put("mangrove_swamp", Biomes.SWAMP);

        //unsure what to do with those, they'll return THE_VOID for now
        //dripstone_caves
        //lush_caves
        //deep_dark
    }

    /**
     * Converts a vanilla biome into its seedfinding equivalent by registry id.
     *
     * @param biome the vanilla biome
     * @return the matching seedfinding biome, or the void biome when unknown
     */
    public static Biome swap(net.minecraft.world.level.biome.Biome biome) {
        ClientPacketListener clientPacketListener = Minecraft.getInstance().getConnection();
        if (clientPacketListener == null) return Biomes.VOID;

        Identifier biomeID = clientPacketListener
                .registryAccess()
                .lookup(Registries.BIOME)
                .map(reg -> reg.getKey(biome))
                .orElse(null);

        if (biomeID == null) return Biomes.THE_VOID;

        return COMPATREGISTRY.getOrDefault(biomeID.getPath(), Biomes.VOID);
    }

    /**
     * Converts a seedfinding biome back into its vanilla equivalent using the vanilla registries.
     *
     * @param biome the seedfinding biome
     * @return the matching vanilla biome, or the vanilla void biome when unknown
     */
    public static net.minecraft.world.level.biome.Biome swap(Biome biome) {
        // internal, meh
        var biomeRegistries = VanillaRegistries.createLookup().lookupOrThrow(Registries.BIOME);

        return biomeRegistries.get(ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace(biome.getName()))).orElse(
                biomeRegistries.getOrThrow(net.minecraft.world.level.biome.Biomes.THE_VOID)
        ).value();
    }
}
