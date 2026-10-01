package com.zephyr.client.cornerstone;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Converts a captured region into origin-relative {@code setblock}/{@code fill} commands.
 * Greedily merges same-state runs along X, then Z, then Y (bottom-up, so callers that
 * feed cells bottom-up get stable placement order).
 */
public final class RegionConverter {
    public static final int MAX_FILL_VOLUME = 32_768;
    private static final int MAX_COMMAND_LENGTH = 256 - 60;
    private static final int USED = -1;

    public record Result(BlockPos min, int sizeX, int sizeY, int sizeZ, List<String> commands, int skipped) {}

    public record Grid(BlockPos min, int sizeX, int sizeY, int sizeZ, int[] cells, List<BlockState> palette) {}

    private RegionConverter() {}

    public static Result convert(Level level, BlockPos a, BlockPos b, boolean includeAir) {
        return convertGrid(snapshot(level, a, b), includeAir);
    }

    public static Grid snapshot(Level level, BlockPos a, BlockPos b) {
        BlockPos min = new BlockPos(
                Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(
                Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));

        int sx = max.getX() - min.getX() + 1;
        int sy = max.getY() - min.getY() + 1;
        int sz = max.getZ() - min.getZ() + 1;

        long volume = (long) sx * sy * sz;
        if (volume > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Region is too large (" + volume + " blocks, max " + Integer.MAX_VALUE + ").");
        }
        if (!level.hasChunksAt(min, max)) {
            throw new IllegalArgumentException(
                    "Part of the region is in unloaded chunks. Move closer (or raise render distance) and try again.");
        }

        int[] cells = new int[(int) volume];
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> indexByState = new IdentityHashMap<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    cursor.set(min.getX() + x, min.getY() + y, min.getZ() + z);
                    BlockState state = level.getBlockState(cursor);
                    Integer idx = indexByState.get(state);
                    if (idx == null) {
                        idx = palette.size();
                        palette.add(state);
                        indexByState.put(state, idx);
                    }
                    cells[x + sx * (z + sz * y)] = idx;
                }
            }
        }
        return new Grid(min, sx, sy, sz, cells, palette);
    }

    public static Result convertGrid(Grid grid, boolean includeAir) {
        int sx = grid.sizeX();
        int sy = grid.sizeY();
        int sz = grid.sizeZ();
        int[] cells = grid.cells();
        List<BlockState> palette = grid.palette();

        List<String> commands = new ArrayList<>();
        int skipped = 0;

        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    int i = x + sx * (z + sz * y);
                    int target = cells[i];
                    if (target == USED) continue;
                    BlockState state = palette.get(target);
                    if (!includeAir && state.isAir()) {
                        cells[i] = USED;
                        continue;
                    }

                    int w = 1;
                    while (x + w < sx && w < MAX_FILL_VOLUME && cells[i + w] == target) w++;

                    int d = 1;
                    while (z + d < sz && (long) w * (d + 1) <= MAX_FILL_VOLUME
                            && rowFree(cells, target, sx, sz, x, y, z + d, w)) d++;

                    int h = 1;
                    while (y + h < sy && (long) w * d * (h + 1) <= MAX_FILL_VOLUME
                            && layerFree(cells, target, sx, sz, x, y + h, z, w, d)) h++;

                    for (int yy = y; yy < y + h; yy++)
                        for (int zz = z; zz < z + d; zz++)
                            for (int xx = x; xx < x + w; xx++)
                                cells[xx + sx * (zz + sz * yy)] = USED;

                    String block = BlockStateParser.serialize(state);
                    String command;
                    if (w == 1 && d == 1 && h == 1) {
                        command = "setblock " + rel(x) + " " + rel(y) + " " + rel(z) + " " + block;
                    } else {
                        command = "fill " + rel(x) + " " + rel(y) + " " + rel(z) + " "
                                + rel(x + w - 1) + " " + rel(y + h - 1) + " " + rel(z + d - 1) + " " + block;
                    }

                    if (command.length() > MAX_COMMAND_LENGTH) {
                        skipped++;
                    } else {
                        commands.add(command);
                    }
                }
            }
        }
        return new Result(grid.min(), sx, sy, sz, commands, skipped);
    }

    private static boolean rowFree(int[] cells, int target,
                                   int sx, int sz, int x, int y, int z, int w) {
        int base = x + sx * (z + sz * y);
        for (int i = 0; i < w; i++) {
            if (cells[base + i] != target) return false;
        }
        return true;
    }

    private static boolean layerFree(int[] cells, int target,
                                     int sx, int sz, int x, int y, int z, int w, int d) {
        for (int dz = 0; dz < d; dz++) {
            if (!rowFree(cells, target, sx, sz, x, y, z + dz, w)) return false;
        }
        return true;
    }

    private static String rel(int v) {
        return v == 0 ? "~" : "~" + v;
    }
}
