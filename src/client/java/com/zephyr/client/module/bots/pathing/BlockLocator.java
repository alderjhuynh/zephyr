package com.zephyr.client.module.bots.pathing;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Scans the loaded chunks around the player for the nearest block of a given
 * type, used by {@code .z path task mine <block>} to find the block the bot
 * should walk to and break. Chunk sections are probed with a palette-level
 * {@link LevelChunkSection#maybeHas} test first, so sections that cannot contain
 * the target are skipped without a per-block sweep.
 */
public final class BlockLocator {

    private BlockLocator() {
    }

    /**
     * Finds the closest loaded block of the given type around the player, or
     * {@code null} when none is loaded within render distance.
     *
     * @param client the Minecraft client instance
     * @param target the block type to look for
     * @return the nearest matching position, or {@code null}
     */
    public static BlockPos findNearest(Minecraft client, Block target) {
        if (client.level == null || client.player == null) return null;
        BlockPos playerPos = client.player.blockPosition();
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        int radius = Math.min(client.options.renderDistance().get() + 1, 16);

        // Scan chunk rings outward so the first ring that contains a match yields
        // the nearest candidate without sweeping the whole search radius.
        for (int ring = 0; ring <= radius; ring++) {
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            int minX = playerChunkX - ring;
            int maxX = playerChunkX + ring;
            int minZ = playerChunkZ - ring;
            int maxZ = playerChunkZ + ring;

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x != minX && x != maxX && z != minZ && z != maxZ) continue;
                    BlockPos found = scanChunk(client, x, z, target, playerPos);
                    if (found != null) {
                        double d = playerPos.distSqr(found);
                        if (d < bestDist) {
                            bestDist = d;
                            best = found;
                        }
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    private static BlockPos scanChunk(Minecraft client, int chunkX, int chunkZ, Block target, BlockPos playerPos) {
        LevelChunk chunk = client.level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null || chunk.isEmpty()) return null;

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        LevelChunkSection[] sections = chunk.getSections();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;
            if (!section.maybeHas(state -> state.getBlock() == target)) continue;

            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 16; y++) {
                        if (section.getBlockState(x, y, z).getBlock() == target) {
                            BlockPos pos = new BlockPos(minX + x, baseY + y, minZ + z);
                            double d = playerPos.distSqr(pos);
                            if (d < bestDist) {
                                bestDist = d;
                                best = pos;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }
}