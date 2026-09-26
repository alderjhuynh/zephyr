package com.zephyr.client.module.qol.seedcracker.finder;

import com.zephyr.client.module.qol.seedcracker.finder.decorator.EndPillarsFinder;
import com.zephyr.client.module.qol.seedcracker.finder.decorator.ore.EmeraldOreFinder;
import com.zephyr.client.module.qol.seedcracker.finder.structure.*;
import com.zephyr.client.module.qol.seedcracker.util.HeightContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * Rebuilds finder search positions for the current world height and re-scans the loaded area.
 */
public class ReloadFinders {
    public Minecraft client = Minecraft.getInstance();

    /**
     * Recomputes {@link Finder#CHUNK_POSITIONS} and the height context for the given vertical
     * range, then rebuilds the derived search positions of every height-dependent finder.
     *
     * @param minY the minimum world Y
     * @param maxY the maximum world Y (exclusive)
     */
    public static void reloadHeight(int minY, int maxY) {
        Finder.CHUNK_POSITIONS.clear();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Finder.CHUNK_POSITIONS.add(new BlockPos(x, y, z));
                }
            }
        }
        Finder.heightContext = new HeightContext(minY, maxY);

        EmeraldOreFinder.reloadSearchPositions();
        EndPillarsFinder.BedrockMarkerFinder.reloadSearchPositions();
        AbstractTempleFinder.reloadSearchPositions();
        BuriedTreasureFinder.reloadSearchPositions();
        EndCityFinder.reloadSearchPositions();
        MonumentFinder.reloadSearchPositions();
        OutpostFinder.reloadSearchPositions();
        IglooFinder.reloadSearchPositions();
        TrialChambersFinder.reloadSearchPositions();
    }

    /**
     * Re-dispatches every chunk within the player's render distance through the finder queue.
     */
    public void reload() {
        int renderdistance = client.options.renderDistance().get();

        int playerChunkX = (int) (Math.round(client.player.getX()) >> 4);
        int playerChunkZ = (int) (Math.round(client.player.getZ()) >> 4);
        for (int i = playerChunkX - renderdistance; i < playerChunkX + renderdistance; i++) {
            for (int j = playerChunkZ - renderdistance; j < playerChunkZ + renderdistance; j++) {
                FinderQueue.get().onChunkData(client.level, new ChunkPos(i, j));
            }
        }
    }
}
