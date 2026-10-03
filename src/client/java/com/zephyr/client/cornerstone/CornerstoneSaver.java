package com.zephyr.client.cornerstone;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.zephyr.Zephyr;
import com.zephyr.client.commands.CommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// 1.21.1 port: sendOverlayMessage -> displayClientMessage(msg, true).
public final class CornerstoneSaver {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "zephyr-cornerstone-save");
        t.setDaemon(true);
        return t;
    });
    private static final int BLOCKS_PER_TICK = 200_000;

    private static Job active;

    private static final class Job {
        final String name;
        final Level level;
        final BlockPos min;
        final BlockPos max;
        final int sx;
        final int sy;
        final int sz;
        final boolean includeAir;
        final int[] cells;
        final List<BlockState> palette = new ArrayList<>();
        final Map<BlockState, Integer> indexByState = new IdentityHashMap<>();
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        final int volume;
        int next;
        int tickCounter;
        Future<?> mergeFuture;
        boolean merging;

        Job(String name, Level level, BlockPos min, BlockPos max,
                int sx, int sy, int sz, boolean includeAir, int[] cells) {
            this.name = name;
            this.level = level;
            this.min = min;
            this.max = max;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.includeAir = includeAir;
            this.cells = cells;
            this.volume = cells.length;
        }
    }

    private CornerstoneSaver() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(CornerstoneSaver::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (active != null) {
                if (active.mergeFuture != null) active.mergeFuture.cancel(true);
                active = null;
            }
        });
    }

    public static boolean isBusy() {
        return active != null;
    }

    public static boolean start(String name, Level level, BlockPos a, BlockPos b, boolean includeAir) {
        if (active != null) return false;

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

        final int[] cells;
        try {
            cells = new int[(int) volume];
        } catch (OutOfMemoryError e) {
            throw new IllegalArgumentException("Region is too large to snapshot in memory (" + volume + " blocks).");
        }
        active = new Job(name, level, min, max, sx, sy, sz, includeAir, cells);
        return true;
    }

    public static int cancel() {
        if (active == null) return 0;
        if (active.mergeFuture != null) active.mergeFuture.cancel(true);
        active = null;
        return 1;
    }

    private static void tick(Minecraft mc) {
        Job job = active;
        if (job == null) return;

        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            if (job.mergeFuture != null) job.mergeFuture.cancel(true);
            active = null;
            return;
        }

        if (job.merging) {
            job.tickCounter++;
            if (job.tickCounter % 20 == 0) {
                player.displayClientMessage(Component.literal("Cornerstone: merging '" + job.name + "'..."), true);
            }
            return;
        }

        int budget = BLOCKS_PER_TICK;
        int minX = job.min.getX();
        int minY = job.min.getY();
        int minZ = job.min.getZ();
        int sx = job.sx;
        int sz = job.sz;
        while (budget-- > 0 && job.next < job.volume) {
            int idx = job.next;
            int x = idx % sx;
            int tmp = idx / sx;
            int z = tmp % sz;
            int y = tmp / sz;
            job.cursor.set(minX + x, minY + y, minZ + z);
            BlockState state = job.level.getBlockState(job.cursor);
            Integer paletteIdx = job.indexByState.get(state);
            if (paletteIdx == null) {
                paletteIdx = job.palette.size();
                job.palette.add(state);
                job.indexByState.put(state, paletteIdx);
            }
            job.cells[idx] = paletteIdx;
            job.next++;
        }

        if (job.next < job.volume) {
            job.tickCounter++;
            if (player.tickCount % 10 == 0 || job.tickCounter == 1) {
                int pct = job.next * 100 / job.volume;
                player.displayClientMessage(Component.literal(
                        "Cornerstone: scanning '" + job.name + "' " + pct + "%"), true);
            }
            return;
        }

        job.merging = true;
        job.tickCounter = 0;
        player.displayClientMessage(Component.literal("Cornerstone: merging '" + job.name + "'..."), true);

        RegionConverter.Grid grid =
                new RegionConverter.Grid(job.min, job.sx, job.sy, job.sz, job.cells, job.palette);
        job.mergeFuture = EXECUTOR.submit(() -> {
            RegionConverter.Result result;
            try {
                result = RegionConverter.convertGrid(grid, job.includeAir);
            } catch (Exception e) {
                Zephyr.LOGGER.error("[Zephyr] Save '{}' failed during merge", job.name, e);
                mc.execute(() -> fail(job, "Save failed: " + e.getMessage()));
                return;
            }
            mc.execute(() -> finish(job, result));
        });
    }

    private static void finish(Job job, RegionConverter.Result result) {
        if (active != job) return;
        active = null;

        SavedRegion region = new SavedRegion();
        region.originX = result.min().getX();
        region.originY = result.min().getY();
        region.originZ = result.min().getZ();
        region.sizeX = result.sizeX();
        region.sizeY = result.sizeY();
        region.sizeZ = result.sizeZ();
        region.commands = result.commands();

        if (!CornerstoneStore.put(job.name, region)) {
            CommandManager.sendMessage("Could not write the save file");
            return;
        }

        String msg = "Saved '" + job.name + "': " + result.sizeX() + "x" + result.sizeY() + "x" + result.sizeZ()
                + " region as " + result.commands().size() + " commands.";
        if (result.skipped() > 0) {
            msg += " (" + result.skipped() + " skipped: command too long)";
        }
        long volume = (long) result.sizeX() * result.sizeY() * result.sizeZ();
        if (volume > 1_000_000) {
            msg += " Warning: large region (" + volume + " blocks)";
        }
        CommandManager.sendMessage(msg);
    }

    private static void fail(Job job, String message) {
        if (active != job) return;
        active = null;
        CommandManager.sendMessage(message);
    }
}
