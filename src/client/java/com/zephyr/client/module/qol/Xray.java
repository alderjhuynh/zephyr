package com.zephyr.client.module.qol;

import com.zephyr.Zephyr;
import com.zephyr.client.commands.CommandManager;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.setting.ListSetting;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.configplusgui.setting.StringSetting;
import com.zephyr.client.module.qol.xray.seed.GenView;
import com.zephyr.client.module.qol.xray.seed.OrePredictor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Outlines selected ores through walls using always-on-top gizmo cuboids.
 *
 * <p>Two modes:
 * <ul>
 *   <li>{@code Block} — scans the loaded blocks in a configurable radius for a preset
 *   ore type (colored per ore) or an arbitrary user-defined block list;</li>
 *   <li>{@code Seed} — knows the world seed (Seedcracker output, or a manual seed
 *   override that takes precedence) and predicts vanilla ore generation directly,
 *   without scanning blocks for ores; the custom list is a block-scan concept and is
 *   ignored in this mode.</li>
 * </ul>
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

    /** Placed feature keys (vanilla ids) whose generation covers each preset ore type. */
    private static final Map<Xray.BlockType, List<String>> SEED_FEATURE_KEYS = Map.ofEntries(
            Map.entry(Xray.BlockType.COAL, List.of("ore_coal_upper", "ore_coal_lower")),
            Map.entry(Xray.BlockType.IRON, List.of("ore_iron_upper", "ore_iron_middle", "ore_iron_small")),
            Map.entry(Xray.BlockType.GOLD, List.of("ore_gold_extra", "ore_gold", "ore_gold_lower")),
            Map.entry(Xray.BlockType.DIAMOND, List.of("ore_diamond", "ore_diamond_medium", "ore_diamond_large", "ore_diamond_buried")),
            Map.entry(Xray.BlockType.EMERALD, List.of("ore_emerald")),
            Map.entry(Xray.BlockType.COPPER, List.of("ore_copper", "ore_copper_large")),
            Map.entry(Xray.BlockType.REDSTONE, List.of("ore_redstone", "ore_redstone_lower")),
            Map.entry(Xray.BlockType.LAPIS, List.of("ore_lapis", "ore_lapis_buried")),
            Map.entry(Xray.BlockType.QUARTZ, List.of("ore_quartz_nether", "ore_quartz_deltas")),
            Map.entry(Xray.BlockType.DEBRIS, List.of("ore_ancient_debris_large", "ore_debris_small"))
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

    public enum Type {
        BLOCK,
        SEED
    }

    private final EnumSetting<Type> type = new EnumSetting<>("Type", Type.BLOCK);

    private final NumberSetting searchRadius =
            new NumberSetting("Search Radius", 24, 1, 30, 1);

    /** Prediction distance around the player in chunks while in Seed mode. */
    private final NumberSetting seedRadius =
            new NumberSetting("Seed Radius", 4, 1, 12, 1);

    /**
     * Manual world seed. Overrides the Seedcracker's recovered seed while non-empty;
     * accepts a signed decimal number (with optional {@code L} suffix or underscores)
     * or a {@code 0x...} hex notation.
     */
    private final StringSetting manualSeed = new StringSetting("Seed", "");

    private final ListSetting customBlocks = new ListSetting("Custom Blocks");

    // Cache for custom list: parsed Block -> color, invalidated when list contents change
    private Map<Block, Integer> cachedCustomColors = Map.of();
    private List<ListSetting.ListEntry> cachedCustomSnapshot = List.of();
    private int cachedCustomHash;

    // ---- Seed mode state ----
    /** Seed recovered by the Seedcracker module (volatile: pushed on a worker thread). */
    private volatile long crackedSeed;
    /** Level/seed/type combination the prediction caches were built for. */
    private ClientLevel seedLevel;
    private long predictionSeed = Long.MIN_VALUE;
    private BlockType seedPredictionType;
    private GenView seedView;
    private OrePredictor seedPredictor;
    /** Chunk key -> predicted features/positions; culled with the level or seed. */
    private final Map<Long, List<OrePredictor.Prediction>> predictionCache = new HashMap<>();
    private final Set<Long> attemptedChunks = new LinkedHashSet<>();
    /** Chunk radius offsets (dx, dz), nearest-first; rebuilt when Seed Radius changes. */
    private List<int[]> seedOffsets = List.of();
    private int seedOffsetRadius = -1;
    private boolean warnedSeedMissing;
    private boolean warnedSeedFailed;
    /** Set once the first prediction sample has been logged for the current state. */
    private boolean loggedFirstPredictions;
    /** Ticks since the last predictor-creation attempt while creation keeps failing. */
    private int seedRetryTicks;
    /** Next retry interval in ticks; doubles per failure (capped) while registries sync. */
    private int seedRetryDelay = 20;

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
        addSetting(type);
        addSetting(seedRadius);
        addSetting(manualSeed);
        Seedcracker.entrypoints.add(this::onWorldSeed);
    }

    /** SeedCrackerAPI callback: stores the recovered world seed. */
    private void onWorldSeed(long seed) {
        this.crackedSeed = seed;
    }

    /**
     * Parses the manual seed setting; {@code null} when empty or malformed, in which
     * case the Seedcracker's recovered seed is used.
     */
    private Long parseManualSeed() {
        String text = this.manualSeed.get();
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.endsWith("L") || t.endsWith("l")) {
            t = t.substring(0, t.length() - 1);
        }
        t = t.replace("_", "");
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(t, 10);
        } catch (NumberFormatException decimalFailed) {
            if (t.startsWith("0X") || t.startsWith("0x")) {
                try {
                    return Long.parseUnsignedLong(t.substring(2), 16);
                } catch (NumberFormatException hexFailed) {
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * Scans the blocks within the search radius and outlines those matching the
     * selected preset or custom list; or predicts the generation for the known
     * world seed when the SEED type is selected.
     *
     * @param client the Minecraft client instance
     */
    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            return;
        }

        Type currentType = type.get();

        if (client.level == null || client.player == null) {
            return;
        }

        if (currentType == Type.BLOCK) {
            this.clearSeedState();
            this.tickBlockScan(client);
        } else {
            // seed based xray, works only with values in the BlockType enum other than the custom list
            this.tickSeed(client);
        }
    }

    private void clearSeedState() {
        this.seedLevel = null;
        this.predictionSeed = Long.MIN_VALUE;
        this.seedPredictionType = null;
        this.seedView = null;
        this.seedPredictor = null;
        this.seedRetryTicks = 0;
        this.seedRetryDelay = 20;
        this.predictionCache.clear();
        this.attemptedChunks.clear();
        this.seedOffsets = List.of();
        this.seedOffsetRadius = -1;
        this.warnedSeedFailed = false;
    }

    private void tickBlockScan(Minecraft client) {
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

    private void tickSeed(Minecraft client) {
        BlockType mode = block.get();
        if (mode == BlockType.LIST) {
            // Custom lists are a block-scan concept; there is nothing to predict from a seed.
            return;
        }
        Long manual = this.parseManualSeed();
        long effectiveSeed = manual != null ? manual : this.crackedSeed;
        if (effectiveSeed == 0) {
            if (!this.warnedSeedMissing) {
                CommandManager.sendMessage("§7[Xray] §fNo world seed available: enable the Seedcracker or enter a manual seed.");
                this.warnedSeedMissing = true;
            }
            return;
        }
        this.warnedSeedMissing = false;

        ClientLevel level = client.level;
        boolean stateChanged = level != this.seedLevel || effectiveSeed != this.predictionSeed || this.seedPredictionType != mode;
        if (stateChanged) {
            this.resetPredictionState(level, effectiveSeed, mode);
            Zephyr.LOGGER.info("[Xray/Seed] building predictor: dimension={}, seed={}, mode={}",
                    level.dimension().identifier(), effectiveSeed, mode);
            this.seedView = new GenView(level, effectiveSeed);
            this.seedPredictor = OrePredictor.create(this.seedView);
            if (this.seedPredictor == null && !this.warnedSeedFailed) {
                CommandManager.sendMessage("§7[Xray] §cSeed prediction unavailable: " + OrePredictor.lastFailureReason());
                this.warnedSeedFailed = true;
            }
        } else if (this.seedPredictor == null) {
            // Registry data may still be arriving while joining; retry with growing
            // delays instead of staying stuck until the seed/type changes.
            if (++this.seedRetryTicks >= this.seedRetryDelay) {
                this.seedRetryTicks = 0;
                this.seedRetryDelay = Math.min(this.seedRetryDelay * 2, 600);
                Zephyr.LOGGER.info("[Xray/Seed] retrying predictor creation (next retry in {} ticks)", this.seedRetryDelay);
                this.seedView = new GenView(level, effectiveSeed);
                this.seedPredictor = OrePredictor.create(this.seedView);
                if (this.seedPredictor != null) {
                    this.seedRetryDelay = 20;
                    CommandManager.sendMessage("§7[Xray] §aSeed prediction ready.");
                }
            }
        }
        if (this.seedPredictor == null) {
            return;
        }

        List<String> wantedKeys = SEED_FEATURE_KEYS.get(mode);
        if (wantedKeys == null) {
            return;
        }
        List<Holder<PlacedFeature>> wanted = this.seedPredictor.resolveWanted(wantedKeys);
        if (wanted.isEmpty()) {
            return;
        }

        int radius = (int) Math.round(searchRadius.get());
        int chunkRadius = Math.max(1, (int) Math.round(seedRadius.get()));
        if (chunkRadius != this.seedOffsetRadius) {
            this.seedOffsets = buildOffsets(chunkRadius);
            this.seedOffsetRadius = chunkRadius;
        }

        BlockPos playerPos = client.player.blockPosition();
        int centerChunkX = playerPos.getX() >> 4;
        int centerChunkZ = playerPos.getZ() >> 4;

        this.predictNewChunks(level, centerChunkX, centerChunkZ, wanted);

        int minX = centerChunkX - chunkRadius;
        int maxX = centerChunkX + chunkRadius;
        int minZ = centerChunkZ - chunkRadius;
        int maxZ = centerChunkZ + chunkRadius;

        int minY = Math.max(level.getMinY(), playerPos.getY() - radius - 256);
        int maxY = Math.min(level.getMaxY() - 1, playerPos.getY() + radius + 256);

        Map<Block, Integer> colors = getPresetMap(mode);
        if (colors.isEmpty()) {
            return;
        }

        try (var ignored = client.collectPerTickGizmos()) {
            for (var chunkEntry : this.predictionCache.entrySet()) {
                long chunkKey = chunkEntry.getKey();
                int chunkX = (int) (chunkKey & 0xFFFFFFFFL);
                int chunkZ = (int) (chunkKey >> 32);
                if (chunkX < minX || chunkX > maxX || chunkZ < minZ || chunkZ > maxZ) {
                    continue;
                }
                for (OrePredictor.Prediction prediction : chunkEntry.getValue()) {
                    for (BlockPos pos : prediction.positions()) {
                        if (pos.getY() < minY || pos.getY() > maxY) {
                            continue;
                        }
                        // Hide mined/carved positions: prediction output is culled against
                        // the live world, so outlines only show still-existing ore.
                        Block live = client.level.getBlockState(pos).getBlock();
                        Integer color = colors.get(live);
                        if (color == null) {
                            continue;
                        }
                        Gizmos.cuboid(pos, GizmoStyle.stroke(color)).setAlwaysOnTop();
                    }
                }
            }
        }
    }

    private void resetPredictionState(ClientLevel level, long seed, BlockType mode) {
        this.seedLevel = level;
        this.predictionSeed = seed;
        this.seedPredictionType = mode;
        this.seedPredictor = null;
        this.seedRetryTicks = 0;
        this.seedRetryDelay = 20;
        this.predictionCache.clear();
        this.attemptedChunks.clear();
        this.warnedSeedFailed = false;
        this.loggedFirstPredictions = false;
    }

    /** Predicts up to a small budget of not-yet-attempted chunks, nearest first. */
    private void predictNewChunks(ClientLevel level, int centerChunkX, int centerChunkZ, List<Holder<PlacedFeature>> wanted) {
        int budget = 3;
        for (int[] offset : this.seedOffsets) {
            if (budget <= 0) {
                break;
            }
            int chunkX = centerChunkX + offset[0];
            int chunkZ = centerChunkZ + offset[1];
            long key = chunkKey(chunkX, chunkZ);
            if (this.attemptedChunks.contains(key)) {
                continue;
            }
            this.attemptedChunks.add(key);
            List<OrePredictor.Prediction> result = this.seedPredictor.predictChunk(chunkX, chunkZ, wanted);
            if (result != null) {
                this.predictionCache.put(key, result);
                budget--;
            }
        }

        // One-shot sample diagnostic: if gizmos never render, this says whether
        // prediction produced nothing (prediction-side) or produced positions that
        // never render (rendering/culling-side).
        if (!this.loggedFirstPredictions && this.attemptedChunks.size() >= 9) {
            int withResults = (int) this.predictionCache.values().stream().filter(l -> !l.isEmpty()).count();
            long positions = this.predictionCache.values().stream().mapToLong(List::size).sum();
            Zephyr.LOGGER.info("[Xray/Seed] prediction sample: {} chunks attempted, {} with results, {} positions total (wanted features: {})",
                    this.attemptedChunks.size(), withResults, positions, wanted.size());
            this.loggedFirstPredictions = true;
        }
    }

    /** Resolves the BlockType's placed features from the synced registry. */
    /** Chunk radius offsets sorted by manhattan distance (the player's chunk first). */
    private static List<int[]> buildOffsets(int radius) {
        List<int[]> offsets = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                offsets.add(new int[]{dx, dz});
            }
        }
        offsets.sort((a, b) -> Integer.compare(Math.abs(a[0]) + Math.abs(a[1]), Math.abs(b[0]) + Math.abs(b[1])));
        return offsets;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkZ << 32) | (chunkX & 0xFFFFFFFFL);
    }
}
