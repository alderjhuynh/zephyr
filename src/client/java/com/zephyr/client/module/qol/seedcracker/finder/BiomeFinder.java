package com.zephyr.client.module.qol.seedcracker.finder;

import com.seedfinding.mcbiome.biome.Biomes;
import com.seedfinding.mccore.version.MCVersion;
import com.zephyr.client.module.qol.Seedcracker;
import com.zephyr.client.module.qol.seedcracker.config.Config;
import com.zephyr.client.module.qol.seedcracker.cracker.BiomeData;
import com.zephyr.client.module.qol.seedcracker.cracker.DataAddedEvent;
import com.zephyr.client.module.qol.seedcracker.render.Cuboid;
import com.zephyr.client.module.qol.seedcracker.util.BiomeFixer;
import com.zephyr.client.module.qol.seedcracker.util.Log;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

public class BiomeFinder extends Finder {

    public BiomeFinder(Level world, ChunkPos chunkPos) {
        super(world, chunkPos);
    }

    public static List<Finder> create(Level world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new BiomeFinder(world, chunkPos));
        return finders;
    }

    @Override
    public List<BlockPos> findInChunk() {
        List<BlockPos> result = new ArrayList<>();

        for (int x = 0; x < 16; x += 4) {
            for (int z = 0; z < 16; z += 4) {
                BlockPos blockPos = this.chunkPos.getWorldPosition().offset(x, 0, z);
                Biome biome;
                if (Config.get().getVersion().isNewerOrEqualTo(MCVersion.v1_15)) {
                    biome = this.world.getNoiseBiome(blockPos.getX() >> 2, 0, blockPos.getZ() >> 2).value();
                } else {
                    biome = this.world.getBiome(blockPos).value();

                }
                com.seedfinding.mcbiome.biome.Biome otherBiome = BiomeFixer.swap(biome);
                if (otherBiome == Biomes.THE_VOID) {
                    continue;
                }

                BiomeData data;
                if (Config.get().getVersion().isNewerOrEqualTo(MCVersion.v1_15)) {
                    data = new BiomeData(otherBiome, blockPos.getX() >> 2, blockPos.getZ() >> 2);
                } else {
                    data = new BiomeData(otherBiome, blockPos.getX(), blockPos.getZ());
                }
                if (Seedcracker.get().getDataStorage().addBiomeData(data, DataAddedEvent.POKE_BIOMES)) {
                    blockPos = this.world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, blockPos).below();
                    if (Config.get().debug) Log.warn(blockPos.toShortString() + ", " + otherBiome.getName());
                    result.add(blockPos);
                }
            }
        }
        result.forEach(pos -> this.cuboids.add(new Cuboid(pos, ARGB.color(51, 204, 128))));

        return result;
    }

    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isOverworld(dimension);
    }

}
