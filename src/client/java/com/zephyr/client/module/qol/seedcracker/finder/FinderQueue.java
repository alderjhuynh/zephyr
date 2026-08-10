package com.zephyr.client.module.qol.seedcracker.finder;

import com.zephyr.client.module.qol.seedcracker.config.Config;
import com.zephyr.client.module.qol.seedcracker.render.Cuboid;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Singleton orchestrator that dispatches chunk data to the active finders.
 *
 * <p>Whenever a chunk arrives, every enabled finder type builds its finders for that chunk on the
 * shared service and runs them off the main thread. Results are accumulated in the
 * {@link FinderControl} and rendered via {@link #renderCuboids()}.
 */
public class FinderQueue {

    private final static FinderQueue INSTANCE = new FinderQueue();
    private static final Logger log = LoggerFactory.getLogger(FinderQueue.class);
    /** Shared executor used for scanning chunks. */
    public static ExecutorService SERVICE = Executors.newFixedThreadPool(5);

    public FinderControl finderControl = new FinderControl();

    private FinderQueue() {
        this.clear();
    }

    /**
     * @return the shared {@link FinderQueue} singleton
     */
    public static FinderQueue get() {
        return INSTANCE;
    }

    /**
     * Runs every active finder type against the given chunk when the module is active.
     *
     * @param world the level the chunk belongs to
     * @param chunkPos the chunk that just arrived
     */
    public void onChunkData(Level world, ChunkPos chunkPos) {
        if (!Config.get().active) return;

        getActiveFinderTypes().forEach(type -> {
            SERVICE.submit(() -> {
                try {
                    List<Finder> finders = type.finderBuilder.build(world, chunkPos);

                    finders.forEach(finder -> {
                        if (finder.isValidDimension(world.dimensionType())) {
                            finder.findInChunk();
                            this.finderControl.addFinder(type, finder);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    /**
     * Renders every discovered cuboid through the per-tick gizmo collector.
     * Call from the module tick, guarded by {@code client.collectPerTickGizmos()}.
     */
    public void renderCuboids() {
        if (Config.get().render == Config.RenderType.OFF) return;

        this.finderControl.getActiveFinders().forEach(finder -> {
            if (finder.shouldRender()) {
                finder.cuboids.forEach(Cuboid::render);
            }
        });
    }

    /**
     * @return the currently enabled finder types
     */
    public List<Finder.Type> getActiveFinderTypes() {
        return Arrays.stream(Finder.Type.values())
                .filter(type -> type.enabled.get())
                .collect(Collectors.toList());
    }

    /**
     * Resets the finder control, dropping all tracked finders.
     */
    public void clear() {
        this.finderControl = new FinderControl();
    }
}
