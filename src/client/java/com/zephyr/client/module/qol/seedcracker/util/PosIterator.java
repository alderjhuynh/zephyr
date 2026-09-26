package com.zephyr.client.module.qol.seedcracker.util;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Builds sets of block positions spanning a rectangular volume.
 */
public class PosIterator {

    /**
     * Enumerates every block position within the axis-aligned box between two corners.
     *
     * @param start the first (min) corner
     * @param end the second (max) corner
     * @return the set of positions in the box
     */
    public static Set<BlockPos> create(BlockPos start, BlockPos end) {
        Set<BlockPos> result = new HashSet<>();

        for (int x = start.getX(); x <= end.getX(); x++) {
            for (int z = start.getZ(); z <= end.getZ(); z++) {
                for (int y = start.getY(); y <= end.getY(); y++) {
                    result.add(new BlockPos(x, y, z));
                }
            }
        }

        return result;
    }

}
