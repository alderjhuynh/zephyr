package com.zephyr.client.module.qol.xray.seed;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.SectionPos;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

import java.util.List;
import java.util.function.Predicate;

/**
 * A read-mostly {@link WorldGenLevel} facade over the {@link ClientLevel}.
 *
 * <p>Seed-based ore prediction re-runs the vanilla decoration random stream for a
 * placed feature, so the placement modifiers need a {@link WorldGenLevel} to query.
 * The real client world answers every read (blocks, biomes, heightmaps, registry
 * access); writes are swallowed, prediction must never mutate the loaded world.
 *
 * <p>The world seed is provided externally (Seedcracker output or manual entry),
 * because the client level never carries the full seed.
 */
public final class GenView implements GenAccess {
    /** Vanilla decoration allows feature writes into a 3x3 chunk area (write radius 1). */
    public static final int WRITE_RADIUS = 1;

    private final ClientLevel level;
    private final long worldSeed;

    /**
     * @param level     the live client level backing all reads
     * @param worldSeed the recovered world seed (cracker or manual)
     */
    public GenView(ClientLevel level, long worldSeed) {
        this.level = level;
        this.worldSeed = worldSeed;
    }

    public ClientLevel clientLevel() {
        return this.level;
    }

    /** Mirrors the vanilla decoration-stage writable zone: write radius 1. */
    @Override
    public boolean canWriteHere(BlockPos pos, int chunkX, int chunkZ) {
        return (
                !this.level.isOutsideBuildHeight(pos.getY())
                        && Math.abs((pos.getX() >> 4) - chunkX) <= WRITE_RADIUS
                        && Math.abs((pos.getZ() >> 4) - chunkZ) <= WRITE_RADIUS
        );
    }

    /** Mirrors vanilla decoration write rejection directly on coordinates. */
    @Override
    public boolean canWriteHere(int x, int y, int z, int chunkX, int chunkZ) {
        return !this.level.isOutsideBuildHeight(y)
                && Math.abs((x >> 4) - chunkX) <= WRITE_RADIUS
                && Math.abs((z >> 4) - chunkZ) <= WRITE_RADIUS;
    }

    /** Replicates {@code WorldgenRandom#setDecorationSeed} for a chunk origin. */
    @Override
    public long decorationSeed(int chunkMinX, int chunkMinZ) {
        WorldgenRandom random = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.generateUniqueSeed()));
        return random.setDecorationSeed(this.worldSeed, chunkMinX, chunkMinZ);
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> state) {
        return state.test(this.level.getBlockState(pos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<net.minecraft.world.level.material.FluidState> state) {
        return state.test(this.level.getFluidState(pos));
    }

    // ---- reads: everything answers from the real client world ----

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.level.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.level.getFluidState(pos);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos pos) {
        return this.level.getBlockEntity(pos);
    }

    @Override
    public List<VoxelShape> getEntityCollisions(Entity entity, AABB box) {
        return this.level.getEntityCollisions(entity, box);
    }

    @Override
    public net.minecraft.world.level.border.WorldBorder getWorldBorder() {
        return this.level.getWorldBorder();
    }

    @Override
    public net.minecraft.world.level.BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        return this.level.getChunkForCollisions(chunkX, chunkZ);
    }

    @Override
    public net.minecraft.world.level.lighting.LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus status, boolean load) {
        return this.level.getChunkSource().getChunk(chunkX, chunkZ, status, load);
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        return this.level.hasChunk(chunkX, chunkZ);
    }

    @Override
    public int getHeight(Heightmap.Types type, int x, int z) {
        return this.level.getHeight(type, x, z);
    }

    @Override
    public int getSkyDarken() {
        return this.level.getSkyDarken();
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.level.getBiomeManager();
    }

    @Override
    public Holder<net.minecraft.world.level.biome.Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return this.level.getUncachedNoiseBiome(x, y, z);
    }

    @Override
    public boolean isClientSide() {
        return this.level.isClientSide();
    }

    @Override
    public int getSeaLevel() {
        return this.level.getSeaLevel();
    }

    @Override
    public net.minecraft.world.level.dimension.DimensionType dimensionType() {
        return this.level.dimensionType();
    }

    @Override
    public net.minecraft.core.RegistryAccess registryAccess() {
        return this.level.registryAccess();
    }

    @Override
    public net.minecraft.world.flag.FeatureFlagSet enabledFeatures() {
        return this.level.enabledFeatures();
    }

    @Override
    public net.minecraft.world.attribute.EnvironmentAttributeReader environmentAttributes() {
        return this.level.environmentAttributes();
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> filter, AABB box, Predicate<? super T> predicate) {
        return this.level.getEntities(filter, box, predicate);
    }

    @Override
    public List<Entity> getEntities(Entity except, AABB box, Predicate<? super Entity> predicate) {
        return this.level.getEntities(except, box, predicate);
    }

    @Override
    public List<? extends Player> players() {
        return this.level.players();
    }

    @Override
    public long nextSubTickCount() {
        return this.level.nextSubTickCount();
    }

    @Override
    public LevelData getLevelData() {
        return this.level.getLevelData();
    }

    @Override
    public net.minecraft.server.MinecraftServer getServer() {
        return this.level.getServer();
    }

    @Override
    public ChunkSource getChunkSource() {
        return this.level.getChunkSource();
    }

    @Override
    public RandomSource getRandom() {
        return this.level.getRandom();
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        // never consulted by ore placement; hand back a fixed, harmless instance
        return new DifficultyInstance(Difficulty.PEACEFUL, 0L, 0L, 0.0F);
    }

    @Override
    public void playSound(Entity source, BlockPos pos, SoundEvent sound, SoundSource sourceCategory, float volume, float pitch) {
        // prediction never emits sound
    }

    @Override
    public void addParticle(ParticleOptions options, double x, double y, double z, double xd, double yd, double zd) {
        // prediction never emits particles
    }

    @Override
    public void levelEvent(Entity entity, int event, BlockPos pos, int data) {
        // prediction never emits level events
    }

    @Override
    public void gameEvent(Holder<GameEvent> event, Vec3 position, GameEvent.Context context) {
        // prediction never emits game events
    }

    @Override
    public <T> ScheduledTick<T> createTick(BlockPos pos, T type, int delay) {
        return this.level.createTick(pos, type, delay);
    }

    @Override
    public <T> ScheduledTick<T> createTick(BlockPos pos, T type, int delay, TickPriority priority) {
        return this.level.createTick(pos, type, delay, priority);
    }

    @Override
    public LevelTickAccess<net.minecraft.world.level.block.Block> getBlockTicks() {
        return this.level.getBlockTicks();
    }

    @Override
    public LevelTickAccess<net.minecraft.world.level.material.Fluid> getFluidTicks() {
        return this.level.getFluidTicks();
    }

    // ---- writes: always refused so the loaded world is never touched ----

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursion) {
        return false; // prediction must never mutate the loaded world
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean move) {
        return false; // prediction must never mutate the loaded world
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean drop, Entity breakingEntity, int limit) {
        return false; // prediction must never mutate the loaded world
    }

    // ---- ServerLevelAccessor: no server exists on the client ----

    @Override
    public net.minecraft.server.level.ServerLevel getLevel() {
        return null; // client side has no server level
    }

    // ---- the whole point of the facade ----

    @Override
    public long getSeed() {
        return this.worldSeed;
    }
}
