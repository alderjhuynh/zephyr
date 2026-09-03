package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.ListSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Outlines selected ores through walls using always-on-top gizmo cuboids.
 * The selection can be a preset ore type (colored per ore) or an arbitrary
 * list of user-defined blocks with custom colors, searched within a
 * configurable radius around the player.
 */
public final class Xray extends Module {
    public static final Xray INSTANCE = new Xray();

    private static final Map<Block, Integer> BLOCKS = Map.ofEntries(
            Map.entry(Blocks.COAL_ORE, 0xFF2a2a2b),
            Map.entry(Blocks.DEEPSLATE_COAL_ORE, 0xFF2a2a2b),
            Map.entry(Blocks.IRON_ORE, 0xFF88817B),
            Map.entry(Blocks.DEEPSLATE_IRON_ORE, 0xFF88817B),
            Map.entry(Blocks.GOLD_ORE, 0xFF91866b),
            Map.entry(Blocks.DEEPSLATE_GOLD_ORE, 0xFF91866b),
            Map.entry(Blocks.DIAMOND_ORE, 0xFF5cdbd5),
            Map.entry(Blocks.DEEPSLATE_DIAMOND_ORE, 0xFF5cdbd5),
            Map.entry(Blocks.EMERALD_ORE, 0xFF2acb58),
            Map.entry(Blocks.DEEPSLATE_EMERALD_ORE, 0xFF2acb58),
            Map.entry(Blocks.COPPER_ORE, 0xFFC06C50),
            Map.entry(Blocks.DEEPSLATE_COPPER_ORE, 0xFFC06C50),
            Map.entry(Blocks.REDSTONE_ORE, 0xFFC32C1F),
            Map.entry(Blocks.DEEPSLATE_REDSTONE_ORE, 0xFFC32C1F),
            Map.entry(Blocks.LAPIS_ORE, 0xFF1F438C),
            Map.entry(Blocks.DEEPSLATE_LAPIS_ORE, 0xFF1F438C),
            Map.entry(Blocks.NETHER_QUARTZ_ORE, 0xFFFFFCF5),
            Map.entry(Blocks.NETHER_GOLD_ORE, 0xFF91866b),
            Map.entry(Blocks.ANCIENT_DEBRIS, 0xFFff0000)
    );

    // Cache for preset BlockType -> Map<Block, color>, built lazily once after registries are available.
    private static Map<BlockType, Map<Block, Integer>> presetCache;

    private static Map<Block, Integer> getPresetMap(BlockType type) {
        if (presetCache != null) {
            Map<Block, Integer> cached = presetCache.get(type);
            if (cached != null) return cached;
        }
        // Build all presets at once
        synchronized (Xray.class) {
            if (presetCache == null) {
                Map<BlockType, Map<Block, Integer>> built = new EnumMap<>(BlockType.class);
                for (BlockType bt : BlockType.values()) {
                    if (bt == BlockType.LIST) continue;
                    String needle = bt.name().toLowerCase();
                    Map<Block, Integer> map = new HashMap<>();
                    for (Map.Entry<Block, Integer> e : BLOCKS.entrySet()) {
                        String path = BuiltInRegistries.BLOCK.getKey(e.getKey()).getPath().toLowerCase();
                        if (path.contains(needle)) {
                            map.put(e.getKey(), e.getValue());
                        }
                    }
                    built.put(bt, Map.copyOf(map));
                }
                presetCache = built;
            }
        }
        Map<Block, Integer> result = presetCache.get(type);
        return result != null ? result : Map.of();
    }

    private final EnumSetting<Xray.BlockType> block = new EnumSetting<>("Mode", Xray.BlockType.COAL);
    /** Which blocks to outline: a preset ore, or a user-defined block list. */
    public enum BlockType {
        COAL,
        IRON,
        GOLD,
        DIAMOND,
        EMERALD,
        COPPER,
        REDSTONE,
        LAPIS,
        QUARTZ,
        DEBRIS,
        LIST
    }

    private final NumberSetting searchRadius =
            new NumberSetting("Search Radius", 24, 1, 30, 1);

    private final ListSetting customBlocks = new ListSetting("Custom Blocks");

    // Cache for custom list: parsed Block -> color, invalidated when list contents change
    private Map<Block, Integer> cachedCustomColors = Map.of();
    private List<ListSetting.ListEntry> cachedCustomSnapshot = List.of();
    private int cachedCustomHash;

    private Map<Block, Integer> getCustomColors() {
        List<ListSetting.ListEntry> current = customBlocks.get();
        int h = current.hashCode();
        if (cachedCustomColors != null && h == cachedCustomHash && cachedCustomSnapshot.equals(current)) {
            return cachedCustomColors;
        }
        Map<Block, Integer> colors = new HashMap<>();
        for (ListSetting.ListEntry entry : current) {
            if (entry.blockName().isBlank()) continue;
            Identifier id = entry.blockName().contains(":")
                    ? Identifier.tryParse(entry.blockName())
                    : Identifier.tryParse("minecraft:" + entry.blockName());
            if (id == null) continue;
            Block b = BuiltInRegistries.BLOCK.getValue(id);
            if (b == Blocks.AIR) continue;
            colors.put(b, ListSetting.parseColor(entry.color(), 0xFFFFFFFF));
        }
        cachedCustomColors = colors.isEmpty() ? Map.of() : Map.copyOf(colors);
        cachedCustomSnapshot = List.copyOf(current);
        cachedCustomHash = h;
        return cachedCustomColors;
    }

    private Xray() {
        super("Xray", "Outlines blocks in a list through walls", Category.QOL);
        addSetting(searchRadius);
        addSetting(block);
        addSetting(customBlocks);
    }

    /**
     * Scans the blocks within the search radius and outlines those matching the
     * selected preset or custom list.
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

        BlockType mode = block.get();
        Map<Block, Integer> targetMap;
        if (mode == Xray.BlockType.LIST) {
            targetMap = getCustomColors();
        } else {
            targetMap = getPresetMap(mode);
        }
        if (targetMap.isEmpty()) return;

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

                        if (!section.maybeHas(state -> targetMap.containsKey(state.getBlock()))) continue;

                        int localMinY = Math.max(0, minY - baseY);
                        int localMaxY = Math.min(15, maxY - baseY);

                        for (int y = localMinY; y <= localMaxY; y++) {
                            for (int x = localMinX; x <= localMaxX; x++) {
                                for (int z = localMinZ; z <= localMaxZ; z++) {
                                    Block b = section.getBlockState(x, y, z).getBlock();
                                    Integer color = targetMap.get(b);
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
