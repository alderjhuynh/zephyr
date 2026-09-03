package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Highlights nearby storage containers in the world with colored outlines.
 * While enabled, a configurable radius around the player is searched each tick
 * and every chest, barrel, and shulker box found is drawn as a stroke cuboid
 * (colors are per container type, including modded shulker variants).
 */
public final class ContainerESP extends Module {
    public static final ContainerESP INSTANCE = new ContainerESP();

    private static final Map<Block, Integer> BLOCKS = Map.ofEntries(
            Map.entry(Blocks.CHEST, 0xFF6e3c1a),
            Map.entry(Blocks.TRAPPED_CHEST, 0xFFa81e14),
            Map.entry(Blocks.BARREL, 0xFF6e3c1a),
            Map.entry(Blocks.SHULKER_BOX, 0xFF941694),
            Map.entry(Blocks.ENDER_CHEST, 0xFFbf05fc)
    );

    private static final int SHULKER_COLOR = 0xFF941694;

    // Lazily built set of all shulker_box variants (includes modded). Built once on first tick after registries are frozen.
    private static Set<Block> shulkerSet;
    private static boolean shulkerInit;

    private static Set<Block> getShulkerSet() {
        if (shulkerInit) return shulkerSet;
        Set<Block> set = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            if (path.endsWith("shulker_box")) {
                set.add(block);
            }
        }
        // Also ensure vanilla shulker box is present even if registry iteration misses it
        set.add(Blocks.SHULKER_BOX);
        shulkerSet = Set.copyOf(set);
        shulkerInit = true;
        return shulkerSet;
    }

    private final NumberSetting searchRadius =
            new NumberSetting("Search Radius", 24, 1, 30, 1);

    private ContainerESP() {
        super("Container ESP", "Outlines containers", Category.QOL);
        addSetting(searchRadius);
    }

    /**
     * Scans the blocks within the search radius and outlines any containers found.
     *
     * @param client the Minecraft client instance
     */
    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            return;
        }

        if (client.level == null || client.player == null) {
            return;
        }

        int radius = (int) Math.round(searchRadius.get());

        BlockPos center = client.player.blockPosition();
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        int worldMinY = client.level.getMinY();
        int worldMaxY = client.level.getMaxY() - 1;
        int minY = Math.max(worldMinY, center.getY() - radius);
        int maxY = Math.min(worldMaxY, center.getY() + radius);
        if (minY > maxY) return;

        Set<Block> shulkers = getShulkerSet();

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        try (var ignored = client.collectPerTickGizmos()) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    LevelChunk chunk = client.level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                    if (chunk == null || chunk.isEmpty()) continue;

                    LevelChunkSection[] sections = chunk.getSections();
                    int chunkMinX = chunk.getPos().getMinBlockX();
                    int chunkMinZ = chunk.getPos().getMinBlockZ();

                    // Pre-compute X/Z local bounds for this chunk (clamped to 0..15)
                    int localMinX = Math.max(0, minX - chunkMinX);
                    int localMaxX = Math.min(15, maxX - chunkMinX);
                    int localMinZ = Math.max(0, minZ - chunkMinZ);
                    int localMaxZ = Math.min(15, maxZ - chunkMinZ);
                    if (localMinX > localMaxX || localMinZ > localMaxZ) continue;

                    for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                        LevelChunkSection section = sections[sectionIndex];
                        if (section == null || section.hasOnlyAir()) continue;

                        int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(sectionIndex));
                        int secMinY = baseY;
                        int secMaxY = baseY + 15;
                        if (secMaxY < minY || secMinY > maxY) continue;

                        // Palette-level cull: skip section if it contains none of the target blocks
                        if (!section.maybeHas(state -> {
                            Block b = state.getBlock();
                            return BLOCKS.containsKey(b) || shulkers.contains(b);
                        })) continue;

                        int localMinY = Math.max(0, minY - baseY);
                        int localMaxY = Math.min(15, maxY - baseY);

                        for (int y = localMinY; y <= localMaxY; y++) {
                            for (int x = localMinX; x <= localMaxX; x++) {
                                for (int z = localMinZ; z <= localMaxZ; z++) {
                                    Block block = section.getBlockState(x, y, z).getBlock();
                                    Integer color = BLOCKS.get(block);
                                    if (color == null && shulkers.contains(block)) {
                                        color = SHULKER_COLOR;
                                    }
                                    if (color != null) {
                                        BlockPos pos = new BlockPos(chunkMinX + x, baseY + y, chunkMinZ + z);
                                        Gizmos.cuboid(pos, GizmoStyle.stroke(color)).setAlwaysOnTop();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
