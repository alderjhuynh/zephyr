package com.zephyr.client.mixin.qol.Seedcracker;

import com.zephyr.client.module.qol.Seedcracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into {@link ClientLevel} wiring the Zephyr Seedcracker module into the
 * client world lifecycle.
 *
 * <p>Resets all collected seed-cracking data when the level disconnects, and
 * reports every biome lookup as {@link Biomes#THE_VOID} so the seed cracker's
 * biome finder can observe structure-biome data without the real biome
 * population interfering.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level {

    /**
     * Constructor required by the {@link Level} superclass.
     *
     * @param properties              the level data
     * @param registryRef             the level's resource key
     * @param registryManager         the registry access
     * @param dimensionEntry          the dimension type holder
     * @param isClient                whether this is a client level
     * @param debugWorld              whether the level is a debug world
     * @param biomeAccess             the biome access seed
     * @param maxChainedNeighborUpdates the max neighbor updates per chain
     */
    protected ClientLevelMixin(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    /**
     * Resets the Seedcracker's collected data and finders when the client level
     * is disconnected.
     *
     * @param reason the disconnect reason
     * @param ci     mixin callback info (unused)
     */
    @Inject(method = "disconnect", at = @At("HEAD"))
    private void disconnect(Component reason, CallbackInfo ci) {
        Seedcracker.get().reset();
    }

    /**
     * Overrides every uncached noise biome query to return {@link Biomes#THE_VOID}.
     *
     * @param x  the x coordinate of the biome column
     * @param y  the y coordinate of the biome column
     * @param z  the z coordinate of the biome column
     * @param ci mixin callback used to substitute the biome holder
     */
    @Inject(method = "getUncachedNoiseBiome", at = @At("HEAD"), cancellable = true)
    private void getGeneratorStoredBiome(int x, int y, int z, CallbackInfoReturnable<Holder<Biome>> ci) {
        var biome = registryAccess().lookupOrThrow(Registries.BIOME).get(Biomes.THE_VOID);
        biome.ifPresent(ci::setReturnValue);
    }
}
