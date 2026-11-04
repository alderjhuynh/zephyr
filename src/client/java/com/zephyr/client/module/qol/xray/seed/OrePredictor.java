package com.zephyr.client.module.qol.xray.seed;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.AbstractOreFeature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.jetbrains.annotations.Nullable;
import com.google.common.collect.ImmutableSet;
import com.zephyr.Zephyr;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Predicts vanilla ore positions for chunks of a known world seed without scanning
 * blocks for ores.
 *
 * <p>This mirrors server-side chunk decoration (26.x): every placed feature in a
 * decoration step is seeded with {@code decorationSeed + globalFeatureIndex +
 * 10000 * stepOrdinal}, where {@code decorationSeed} derives deterministically from
 * the world seed and the chunk origin. Each feature owns an independent random
 * stream from that moment, so predicting one ore feature needs no replay of its
 * sibling features; it only needs the feature's global index inside the
 * ore step (recovered here with the real {@link FeatureSorter} over the synced
 * biome and placed-feature registries) plus the client's own terrain for every
 * terrain-dependent check (biome filters, replacement targets, neighbor-air checks
 * and the ocean-floor height probe).
 *
 * <p>Differences from a real server run are bounded and intentional:
 * <ul>
 *   <li>block-replacement tests run against a synthesized "fresh generation" base
 *   state ({@code stone}/{@code deepslate}/{@code netherrack}) because the client
 *   data already contains the finished veins; the synthesized state hits the same
 *   element of vanilla's replaceable rule sets that the real base would;</li>
 *   <li>because the client world already contains finished decoration, positions whose
 *   live block is air (carved caves, mined ore) are rejected, exactly like the
 *   server generation would have rejected carved air;</li>
 * </ul>
 */
public final class OrePredictor {

    /** Vanilla Decoration.UNDERGROUND_ORES — every regular ore feature's step. */
    static final GenerationStep.Decoration ORE_STEP = GenerationStep.Decoration.UNDERGROUND_ORES;

    /** Reason for the most recent failed {@link #create} attempt (diagnostics). */
    private static String lastFailure = "not attempted yet";

    /** Diagnostic reason for the most recent failed {@link #create} attempt. */
    public static String lastFailureReason() {
        return lastFailure;
    }

    /** One predicted feature's result for a chunk. */
    public record Prediction(Holder<PlacedFeature> feature, List<BlockPos> positions) {}

    private final GenAccess view;
    private final StubChunkGenerator generator;
    /** PlacedFeature (by identity) -> global feature index inside the ore step. */
    private final Map<PlacedFeature, Integer> featureIndexes;
    private final boolean overworldStyle;
    /**
     * The biome lookup the feature graph was built from: synced registry when the
     * server sent usable worldgen data, the built-in vanilla lookup otherwise.
     * Chunk-gathered biomes are remapped into it by key so holder comparisons match.
     */
    private final HolderLookup<Biome> biomeLookup;
    /** The placed-feature lookup belonging to the same worldgen source as {@link #biomeLookup}. */
    private final HolderLookup<PlacedFeature> featureLookup;
    /** Placed-feature ids that could not be resolved in {@link #featureLookup} (logged once). */
    private final Set<String> missingFeatures = new LinkedHashSet<>();

    OrePredictor(GenAccess view, Map<PlacedFeature, Integer> featureIndexes, boolean overworldStyle,
                 HolderLookup<Biome> biomeLookup, HolderLookup<PlacedFeature> featureLookup) {
        this.view = view;
        this.biomeLookup = biomeLookup;
        this.featureLookup = featureLookup;
        this.generator = new StubChunkGenerator(view, this::sourceBiome);
        this.featureIndexes = featureIndexes;
        this.overworldStyle = overworldStyle;
    }

    /**
     * Resolves placed-feature ids against this predictor's lookup. Ids absent from
     * the lookup are logged once and skipped — e.g. a server whose datapack removed
     * a vanilla ore.
     */
    public List<Holder<PlacedFeature>> resolveWanted(List<String> names) {
        List<Holder<PlacedFeature>> resolved = new ArrayList<>(names.size());
        for (String name : names) {
            var holder = this.featureLookup.get(ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace(name)));
            if (holder.isPresent()) {
                resolved.add(holder.get());
            } else if (this.missingFeatures.add(name)) {
                Zephyr.LOGGER.warn("[Xray/Seed] placed feature not present in predictor lookup: minecraft:{}", name);
            }
        }
        return resolved;
    }

    /**
     * Creates a predictor for the given client level: resolves the dimension's
     * multi-noise biome preset, orders its possible biomes exactly like
     * {@link net.minecraft.world.level.biome.BiomeSource} does, and runs the real
     * {@link FeatureSorter} to recover every placed feature's global index inside
     * the ore decoration step.
     *
     * <p>All worldgen data (preset, biomes, placed features) comes from one
     * consistent registry access, since {@link PlacedFeature} matching relies on
     * holder identity. Servers are free to strip worldgen data from the login sync;
     * when the synced data is missing or carries no feature lists, the predictor
     * falls back to the client's built-in vanilla worldgen — correct as long as the
     * server still runs vanilla-style generation.
     *
     * @return the predictor, or {@code null} when neither the synced data nor the
     * vanilla fallback can be built (e.g. before login completes)
     */
    public static OrePredictor create(GenView view) {
        var level = view.clientLevel();
        String dimension = String.valueOf(level.dimension().identifier());
        boolean overworldStyle = level.dimension() != Level.NETHER;
        ResourceKey<MultiNoiseBiomeSourceParameterList> key =
                overworldStyle ? MultiNoiseBiomeSourceParameterLists.OVERWORLD : MultiNoiseBiomeSourceParameterLists.NETHER;

        // 1) Prefer the server-synced data: it reflects the server's actual datapack.
        WorldSource source = syncedSource(level, key);
        // 2) Fall back to the client's built-in vanilla worldgen: the preset climate
        //    parameters and biome feature lists are fully defined client-side, so the
        //    complete vanilla feature graph can be rebuilt without any server data.
        if (source == null) {
            source = vanillaSource(overworldStyle);
        }
        if (source == null) {
            lastFailure = "cannot build " + key.identifier() + ": no usable worldgen source (vanilla bootstrap failed)";
            Zephyr.LOGGER.warn("[Xray/Seed] predictor unavailable: {} (dimension={})", lastFailure, dimension);
            return null;
        }

        List<Holder<Biome>> possibleBiomes = presetBiomes(source.preset());
        if (possibleBiomes.isEmpty()) {
            lastFailure = "preset " + key.identifier() + " resolved to zero biomes (" + source.name() + ")";
            Zephyr.LOGGER.warn("[Xray/Seed] predictor unavailable: {} (dimension={})", lastFailure, dimension);
            return null;
        }
        List<FeatureSorter.StepFeatureData> perStep =
                FeatureSorter.buildFeaturesPerStep(possibleBiomes, b -> b.value().getGenerationSettings().features(), true);
        if (ORE_STEP.ordinal() >= perStep.size()) {
            lastFailure = "feature graph has only " + perStep.size() + " decoration steps; ore step "
                    + ORE_STEP.ordinal() + " missing (" + source.name() + ")";
            Zephyr.LOGGER.warn("[Xray/Seed] predictor unavailable: {} (dimension={}, biomes={})",
                    lastFailure, dimension, possibleBiomes.size());
            return null;
        }
        FeatureSorter.StepFeatureData oreStep = perStep.get(ORE_STEP.ordinal());
        Map<PlacedFeature, Integer> indexes = oreStep.features().stream().collect(
                Collectors.toMap(java.util.function.Function.identity(), oreStep.indexMapping()::applyAsInt, (a, b) -> a, IdentityHashMap::new)
        );
        lastFailure = "not attempted yet";
        Zephyr.LOGGER.info("[Xray/Seed] predictor ready: dimension={}, overworldStyle={}, source={}, biomes={}, oreStepFeatures={}",
                dimension, overworldStyle, source.name(), possibleBiomes.size(), oreStep.features().size());
        return new OrePredictor(view, indexes, overworldStyle, source.biomes(), source.features());
    }

    /** One consistent worldgen view: everything from a single registry access. */
    private record WorldSource(String name, HolderLookup<Biome> biomes, HolderLookup<PlacedFeature> features,
                               MultiNoiseBiomeSourceParameterList preset) {
    }

    /**
     * Builds the synced-worldgen source, or {@code null} when this server stripped
     * any part of it (preset registry absent, biomes without feature lists).
     */
    private static @Nullable WorldSource syncedSource(ClientLevel level,
            ResourceKey<MultiNoiseBiomeSourceParameterList> key) {
        HolderLookup<Biome> biomes;
        HolderLookup<PlacedFeature> features;
        HolderLookup<MultiNoiseBiomeSourceParameterList> presetRegistry;
        try {
            biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
            features = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
            presetRegistry = level.registryAccess().lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        } catch (IllegalStateException unavailable) {
            Zephyr.LOGGER.warn("[Xray/Seed] synced worldgen registries incomplete ({}); using built-in vanilla worldgen",
                    unavailable.getMessage());
            return null;
        }
        var presetHolder = presetRegistry.get(key);
        if (presetHolder.isEmpty()) {
            boolean hasOverworld = presetRegistry.get(MultiNoiseBiomeSourceParameterLists.OVERWORLD).isPresent();
            boolean hasNether = presetRegistry.get(MultiNoiseBiomeSourceParameterLists.NETHER).isPresent();
            Zephyr.LOGGER.warn("[Xray/Seed] preset {} missing from synced registry (hasOverworld={}, hasNether={}); using built-in vanilla worldgen",
                    key.identifier(), hasOverworld, hasNether);
            return null;
        }
        if (featuresStripped(presetBiomes(presetHolder.get().value()))) {
            Zephyr.LOGGER.warn("[Xray/Seed] synced biomes carry no feature lists; using built-in vanilla worldgen");
            return null;
        }
        return new WorldSource("server-synced", biomes, features, presetHolder.get().value());
    }

    /** The built-in vanilla worldgen source, cached for the client's lifetime. */
    private static volatile @Nullable HolderLookup.Provider vanillaProvider;

    private static @Nullable WorldSource vanillaSource(boolean overworldStyle) {
        HolderLookup.Provider provider = vanillaProvider;
        if (provider == null) {
            try {
                provider = VanillaRegistries.createWorldLookup();
            } catch (RuntimeException bootstrapFailed) {
                Zephyr.LOGGER.warn("[Xray/Seed] vanilla worldgen bootstrap failed: {}", bootstrapFailed.toString());
                return null;
            }
            vanillaProvider = provider;
        }
        HolderLookup<Biome> biomes = provider.lookupOrThrow(Registries.BIOME);
        MultiNoiseBiomeSourceParameterList.Preset preset = overworldStyle
                ? MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD
                : MultiNoiseBiomeSourceParameterList.Preset.NETHER;
        return new WorldSource("built-in vanilla", biomes, provider.lookupOrThrow(Registries.PLACED_FEATURE),
                new MultiNoiseBiomeSourceParameterList(preset, biomes));
    }

    /** Distinct, insertion-ordered biomes of a preset, mirroring {@code BiomeSource#possibleBiomes}. */
    private static List<Holder<Biome>> presetBiomes(MultiNoiseBiomeSourceParameterList preset) {
        return preset.parameters().values().stream()
                .<Holder<Biome>>map(Pair::getSecond)
                .distinct()
                .collect(ImmutableSet.toImmutableSet())
                .stream()
                .toList();
    }

    /** Whether none of the biomes carries any placed feature (server stripped worldgen sync). */
    private static boolean featuresStripped(List<Holder<Biome>> biomes) {
        return biomes.stream().allMatch(b -> b.value().getGenerationSettings().features().isEmpty());
    }

    /**
     * Predicts the given placed ore features for one chunk.
     *
     * @param chunkX         chunk coordinate
     * @param chunkZ         chunk coordinate
     * @param wantedFeatures placed features to predict (unknown ones are ignored)
     * @return one {@link Prediction} per feature that produced positions (in global
     * index order), or {@code null} when the chunk's data is not available
     */
    public @Nullable List<Prediction> predictChunk(int chunkX, int chunkZ, List<Holder<PlacedFeature>> wantedFeatures) {
        return this.predictChunk(chunkX, chunkZ, wantedFeatures, true);
    }

    /**
     * Test hook: {@code checkBiomes=false} lets synthetic features (not registered in
     * any biome's generation settings) run through the parity harness.
     */
    List<Prediction> predictChunk(int chunkX, int chunkZ, List<Holder<PlacedFeature>> wantedFeatures, boolean checkBiomes) {
        ChunkAccess chunk = this.view.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null) {
            return null;
        }

        Set<Holder<Biome>> areaBiomes = this.gatherBiomes(chunkX, chunkZ);
        BlockPos origin = SectionPos.of(new ChunkPos(chunkX, chunkZ), this.view.getMinSectionY()).origin();
        long decorationSeed = this.view.decorationSeed(origin.getX(), origin.getZ());

        WorldgenRandom random = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.generateUniqueSeed()));
        Map<PlacedFeature, Prediction> predictions = new LinkedHashMap<>();
        LongOpenHashSet predicted = new LongOpenHashSet();
        // Consumes features in global index order, mirroring applyBiomeDecoration's
        // sorted iteration (needed so overlapping veins resolve first-write-wins).
        List<Holder<PlacedFeature>> ordered = new ArrayList<>(wantedFeatures);
        ordered.sort((a, b) -> index(this.featureIndexes, a) - index(this.featureIndexes, b));
        for (Holder<PlacedFeature> featureHolder : ordered) {
            PlacedFeature placedFeature = featureHolder.value();
            Integer globalIndex = this.featureIndexes.get(placedFeature);
            if (globalIndex == null) {
                continue;
            }
            if (checkBiomes && !this.anyBiomeRuns(areaBiomes, featureHolder)) {
                continue;
            }
            List<BlockPos> positions = new ArrayList<>();
            random.setFeatureSeed(decorationSeed, globalIndex, ORE_STEP.ordinal());
            this.replayPlacements(placedFeature, random, origin, chunkX, chunkZ,
                    pos -> {
                        if (predicted.add(pos.asLong())) {
                            positions.add(pos);
                        }
                    });
            if (!positions.isEmpty()) {
                predictions.put(placedFeature, new Prediction(featureHolder, positions));
            }
        }
        return new ArrayList<>(predictions.values());
    }

    private static int index(Map<PlacedFeature, Integer> indexes, Holder<PlacedFeature> holder) {
        Integer i = indexes.get(holder.value());
        return i == null ? Integer.MAX_VALUE : i;
    }

    private boolean anyBiomeRuns(Set<Holder<Biome>> biomes, Holder<PlacedFeature> feature) {
        for (Holder<Biome> biome : biomes) {
            var steps = biome.value().getGenerationSettings().features();
            if (ORE_STEP.ordinal() < steps.size() && steps.get(ORE_STEP.ordinal()).contains(feature)) {
                return true;
            }
        }
        return false;
    }

    /** Collects the biome set of every section in the 3x3 chunk area, like vanilla decoration does. */
    private Set<Holder<Biome>> gatherBiomes(int chunkX, int chunkZ) {
        Set<Holder<Biome>> biomes = new LinkedHashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkAccess chunk = this.view.getChunk(chunkX + dx, chunkZ + dz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                for (var section : chunk.getSections()) {
                    if (section != null) {
                        section.getBiomes().getAll(h -> biomes.add(this.sourceBiome(h)));
                    }
                }
            }
        }
        return biomes;
    }

    /**
     * Maps a chunk's synced biome holder into the predictor's biome lookup by key so
     * feature-membership checks compare identical holder instances. Biomes missing
     * from the lookup (e.g. custom server biomes vs the vanilla fallback) fall back
     * to the synced holder, whose own feature list simply won't match.
     */
    private Holder<Biome> sourceBiome(Holder<Biome> chunkBiome) {
        return chunkBiome.unwrapKey()
                .flatMap(key -> this.biomeLookup.get(key))
                .map(holder -> (Holder<Biome>) holder)
                .orElse(chunkBiome);
    }

    /**
     * Replicates vanilla {@code FeaturePlacer#place} (the with-biome-check path),
     * driving the real placement modifiers with the seeded random and evaluating the
     * ore feature at every final position.
     */
    private void replayPlacements(PlacedFeature placedFeature, RandomSource random, BlockPos origin, int chunkX, int chunkZ, Consumer<BlockPos> out) {
        PlacementContext context = new PlacementContext(this.view, this.generator, Optional.of(placedFeature));
        List<PlacementModifier> placement = placedFeature.placement();
        if (placement.isEmpty()) {
            this.placeOre(placedFeature, random, origin, chunkX, chunkZ, out);
            return;
        }
        ArrayDeque<BlockPos> positions = new ArrayDeque<>();
        ArrayDeque<Integer> modifierIndices = new ArrayDeque<>();
        List<BlockPos> modifiedPositions = new ArrayList<>();
        positions.push(origin);
        modifierIndices.push(0);

        while (!positions.isEmpty()) {
            BlockPos pos = positions.pop();
            int modifierIndex = modifierIndices.pop();
            PlacementModifier modifier = placement.get(modifierIndex);
            modifiedPositions.clear();
            modifier.modify(context, random, pos, modifiedPositions::add);
            int nextModifierIndex = modifierIndex + 1;
            if (nextModifierIndex < placement.size()) {
                // Vanilla pushes in reverse to preserve LIFO order; copy exactly.
                for (int i = modifiedPositions.size() - 1; i >= 0; i--) {
                    positions.push(modifiedPositions.get(i));
                    modifierIndices.push(nextModifierIndex);
                }
            } else {
                for (BlockPos nextPos : modifiedPositions) {
                    this.placeOre(placedFeature, random, nextPos, chunkX, chunkZ, out);
                }
            }
        }
    }

    /** Dispatches between the two vanilla ore feature shapes. */
    private void placeOre(PlacedFeature placedFeature, RandomSource random, BlockPos origin, int chunkX, int chunkZ, Consumer<BlockPos> out) {
        if (placedFeature.feature().value() instanceof OreFeature vein) {
            this.placeVein(vein, random, origin, chunkX, chunkZ, out);
        } else if (placedFeature.feature().value() instanceof ScatteredOreFeature scattered) {
            this.placeScatteredOre(scattered, random, origin, chunkX, chunkZ, out);
        }
    }

    /** Replicates {@code ScatteredOreFeature#place}. */
    private void placeScatteredOre(ScatteredOreFeature feature, RandomSource random, BlockPos origin, int chunkX, int chunkZ, Consumer<BlockPos> out) {
        int numberOfTries = random.nextInt(feature.size() + 1);
        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < numberOfTries; i++) {
            int maxDist = Math.min(i, 7);
            int xd = this.randomPlacementInOneAxis(random, maxDist);
            int yd = this.randomPlacementInOneAxis(random, maxDist);
            int zd = this.randomPlacementInOneAxis(random, maxDist);
            targetPos.setWithOffset(origin, xd, yd, zd);
            if (this.canPlaceOre(targetPos, feature, random)) {
                BlockPos immutable = targetPos.immutable();
                if (this.view.canWriteHere(immutable, chunkX, chunkZ)) {
                    out.accept(immutable);
                }
            }
        }
    }

    private int randomPlacementInOneAxis(RandomSource random, int maxDistanceFromOrigin) {
        return Math.round((random.nextFloat() - random.nextFloat()) * maxDistanceFromOrigin);
    }

    /** Replicates {@code OreFeature#place}: style per-size vein geometry plus the OCEAN_FLOOR_WG probe. */
    private void placeVein(OreFeature feature, RandomSource random, BlockPos origin, int chunkX, int chunkZ, Consumer<BlockPos> out) {
        float dir = random.nextFloat() * (float) Math.PI;
        float spreadXY = feature.size() / 8.0F;
        int maxRadius = Mth.ceil((feature.size() / 16.0F * 2.0F + 1.0F) / 2.0F);
        double x0 = origin.getX() + Math.sin(dir) * spreadXY;
        double x1 = origin.getX() - Math.sin(dir) * spreadXY;
        double z0 = origin.getZ() + Math.cos(dir) * spreadXY;
        double z1 = origin.getZ() - Math.cos(dir) * spreadXY;
        double y0 = origin.getY() + random.nextInt(3) - 2;
        double y1 = origin.getY() + random.nextInt(3) - 2;
        int xStart = origin.getX() - Mth.ceil(spreadXY) - maxRadius;
        int yStart = origin.getY() - 2 - maxRadius;
        int zStart = origin.getZ() - Mth.ceil(spreadXY) - maxRadius;
        int sizeXZ = 2 * (Mth.ceil(spreadXY) + maxRadius);

        if (this.veinFitsAboveFloor(yStart, xStart, zStart, sizeXZ)) {
            this.doPlaceVein(feature, random, x0, x1, z0, z1, y0, y1, xStart, yStart, zStart, sizeXZ, chunkX, chunkZ, out);
        }
    }

    /**
     * Replicates the OCEAN_FLOOR_WG probe in {@code OreFeature#place}: the vein runs
     * once any probed column's floor height is at or above {@code yStart}.
     */
    private boolean veinFitsAboveFloor(int yStart, int xStart, int zStart, int sizeXZ) {
        for (int xprobe = xStart; xprobe <= xStart + sizeXZ; xprobe++) {
            for (int zprobe = zStart; zprobe <= zStart + sizeXZ; zprobe++) {
                if (yStart <= this.floorHeight(xprobe, zprobe)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Ocean-floor height of a single column; the owning chunk must be available. */
    private int floorHeight(int x, int z) {
        ChunkAccess chunk = this.view.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false);
        if (chunk == null) {
            return Integer.MIN_VALUE;
        }
        return this.oceanFloorHeight(chunk, x, z);
    }

    /**
     * Replicates the {@code OCEAN_FLOOR_WG} heightmap by scanning the chunk's own
     * sections top-down (the client never primes WG heightmaps). Water columns fall
     * through to the actual floor, matching the placement probe's intent.
     */
    private int oceanFloorHeight(ChunkAccess chunk, int x, int z) {
        int relX = x & 15;
        int relZ = z & 15;
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = sections.length - 1; i >= 0; i--) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            for (int y = 15; y >= 0; y--) {
                if (!section.getBlockState(relX, y, relZ).isAir()) {
                    return baseY + y + 1;
                }
            }
        }
        return this.view.getMinY() - 1;
    }

    /** Replicates {@code OreFeature#doPlace}. */
    private void doPlaceVein(OreFeature feature, RandomSource random, double x0, double x1, double z0, double z1, double y0, double y1,
                             int xStart, int yStart, int zStart, int sizeXZ, int chunkX, int chunkZ, Consumer<BlockPos> out) {
        int sizeY = 2 * (2 + Mth.ceil((feature.size() / 16.0F * 2.0F + 1.0F) / 2.0F));
        BitSet tested = new BitSet(sizeXZ * sizeY * sizeXZ);
        BlockPos.MutableBlockPos orePos = new BlockPos.MutableBlockPos();
        double[] data = new double[feature.size() * 4];

        for (int i = 0; i < feature.size(); i++) {
            float step = (float) i / feature.size();
            double xx = Mth.lerp(step, x0, x1);
            double yy = Mth.lerp(step, y0, y1);
            double zz = Mth.lerp(step, z0, z1);
            double ss = random.nextDouble() * feature.size() / 16.0;
            double r = ((Mth.sin((float) Math.PI * step) + 1.0F) * ss + 1.0) / 2.0;
            data[i * 4 + 0] = xx;
            data[i * 4 + 1] = yy;
            data[i * 4 + 2] = zz;
            data[i * 4 + 3] = r;
        }

        for (int a = 0; a < feature.size() - 1; a++) {
            if (!(data[a * 4 + 3] <= 0.0)) {
                for (int b = a + 1; b < feature.size(); b++) {
                    if (!(data[b * 4 + 3] <= 0.0)) {
                        double dx = data[a * 4 + 0] - data[b * 4 + 0];
                        double dy = data[a * 4 + 1] - data[b * 4 + 1];
                        double dz = data[a * 4 + 2] - data[b * 4 + 2];
                        double dr = data[a * 4 + 3] - data[b * 4 + 3];
                        if (dr * dr > dx * dx + dy * dy + dz * dz) {
                            if (dr > 0.0) {
                                data[b * 4 + 3] = -1.0;
                            } else {
                                data[a * 4 + 3] = -1.0;
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < feature.size(); i++) {
            double r = data[i * 4 + 3];
            if (!(r < 0.0)) {
                double xx = data[i * 4 + 0];
                double yy = data[i * 4 + 1];
                double zz = data[i * 4 + 2];
                int xMin = Math.max(Mth.floor(xx - r), xStart);
                int yMin = Math.max(Mth.floor(yy - r), yStart);
                int zMin = Math.max(Mth.floor(zz - r), zStart);
                int xMax = Math.max(Mth.floor(xx + r), xMin);
                int yMax = Math.max(Mth.floor(yy + r), yMin);
                int zMax = Math.max(Mth.floor(zz + r), zMin);

                for (int x = xMin; x <= xMax; x++) {
                    double xd = (x + 0.5 - xx) / r;
                    if (xd * xd < 1.0) {
                        for (int y = yMin; y <= yMax; y++) {
                            double yd = (y + 0.5 - yy) / r;
                            if (xd * xd + yd * yd < 1.0) {
                                for (int z = zMin; z <= zMax; z++) {
                                    double zd = (z + 0.5 - zz) / r;
                                    if (xd * xd + yd * yd + zd * zd < 1.0) {
                                        if (!this.view.isOutsideBuildHeight(y)) {
                                            int bitSetIndex = x - xStart + (y - yStart) * sizeXZ + (z - zStart) * sizeXZ * sizeY;
                                            if (!tested.get(bitSetIndex)) {
                                                tested.set(bitSetIndex);
                                                orePos.set(x, y, z);
                                                if (this.view.canWriteHere(x, y, z, chunkX, chunkZ)) {
                                                    if (this.canPlaceOre(orePos, feature, random)) {
                                                        out.accept(orePos.immutable());
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
            }
        }
    }

    /**
     * Replicates the target loop of {@code OreFeature#doPlace}/{@code ScatteredOreFeature#place}
     * against the live block state. Target order, adjacent-air reject and every random
     * draw match vanilla exactly.
     *
     * <p>The live client state equals the fresh-generation state except where features
     * replaced it — and vanilla's target tests fail on those replaced states too (an
     * ore cell is no longer stone), so testing live state keeps both the RNG stream
     * and the placement outcome aligned. Only mined-away positions (live air where
     * fresh generation had ore) diverge: they are rejected before the target loop,
     * which vanilla would only have skipped after consuming the discard roll.
     */
    private boolean canPlaceOre(BlockPos pos, AbstractOreFeature feature, RandomSource random) {
        BlockState state = this.view.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            // Fresh generation could never place into a carver gap or fluid, and a
            // mined-away cell has no present counterpart. Rejecting here consumes no
            // random values, keeping the run's random stream aligned.
            return false;
        }
        // The live world cannot answer "was this stone at decoration time": on a
        // vanilla client the ore is already placed at the position, and antixray
        // servers fake ores into every stone block — the vanilla target-state test
        // would reject everything. Any solid, non-fluid block therefore counts as
        // pristine terrain. The air-exposure discard still mirrors vanilla, since
        // carver air survives into the live world.
        float discardChance = feature.discardChanceOnAirExposure();
        boolean skipAirCheck = discardChance <= 0.0F || (discardChance >= 1.0F ? false : random.nextFloat() >= discardChance);
        return skipAirCheck || !this.isAdjacentToAir(pos);
    }

    /** Replicates {@code AbstractOreFeature#isAdjacentToAir}/{@code #checkNeighbors} against the live terrain. */
    private boolean isAdjacentToAir(BlockPos pos) {
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            neighborPos.setWithOffset(pos, direction);
            if (this.view.getBlockState(neighborPos).isAir()) {
                return true;
            }
        }
        return false;
    }
}
