package com.zephyr.client.mixin.qol.Seedcracker;

import com.zephyr.client.module.qol.Seedcracker;
import com.zephyr.client.module.qol.seedcracker.config.Config;
import com.zephyr.client.module.qol.seedcracker.cracker.DataAddedEvent;
import com.zephyr.client.module.qol.seedcracker.cracker.HashedSeedData;
import com.zephyr.client.module.qol.seedcracker.finder.FinderQueue;
import com.zephyr.client.module.qol.seedcracker.finder.ReloadFinders;
import com.zephyr.client.module.qol.seedcracker.util.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link ClientPacketListener} feeding the Zephyr Seedcracker module
 * with world data as it arrives.
 *
 * <p>Feeds each received chunk to the {@link FinderQueue}, registers the world
 * seed (as a hashed seed) when joining or respawning, and reloads the finder
 * height bounds whenever the dimension changes.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    /** The client level the listener is bound to, used for chunk scanning. */
    @Shadow
    private ClientLevel level;

    /**
     * Handles a freshly loaded chunk by forwarding it to the Seedcracker finder
     * queue for structure scanning.
     *
     * @param packet the chunk data packet received from the server
     * @param ci     mixin callback info (unused)
     */
    @Inject(method = "handleLevelChunkWithLight", at = @At(value = "TAIL"))
    private void onChunkData(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        int chunkX = packet.getX();
        int chunkZ = packet.getZ();
        FinderQueue.get().onChunkData(this.level, new ChunkPos(chunkX, chunkZ));
    }

    /**
     * Registers the world seed when the player joins a server.
     *
     * @param packet the login packet carrying the world spawn info and seed
     * @param ci     mixin callback info (unused)
     */
    @Inject(method = "handleLogin", at = @At(value = "TAIL"))
    public void onGameJoin(ClientboundLoginPacket packet, CallbackInfo ci) {
        newDimension(new HashedSeedData(packet.commonPlayerSpawnInfo().seed()), false);
    }

    /**
     * Registers the world seed when the player respawns or changes dimension.
     *
     * @param packet the respawn packet carrying the world spawn info and seed
     * @param ci     mixin callback info (unused)
     */
    @Inject(method = "handleRespawn", at = @At(value = "TAIL"))
    public void onPlayerRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        newDimension(new HashedSeedData(packet.commonPlayerSpawnInfo().seed()), true);
    }

    /**
     * Records a newly encountered dimension: reloads the finder height bounds
     * for the dimension and stores the hashed seed. On a real dimension change
     * a log line is emitted when the hashed seed was newly added.
     *
     * @param hashedSeedData the hashed seed for the new dimension
     * @param dimensionChange whether this is a respawn/dimension switch
     */
    @Unique
    private void newDimension(HashedSeedData hashedSeedData, boolean dimensionChange) {
        DimensionType dimension = Minecraft.getInstance().level.dimensionType();
        ReloadFinders.reloadHeight(dimension.minY(), dimension.minY() + dimension.logicalHeight());

        if (Seedcracker.get().getDataStorage().addHashedSeedData(hashedSeedData, DataAddedEvent.POKE_BIOMES) && Config.get().active && dimensionChange) {
            Log.error(Log.translate("fetchedHashedSeed"));
            if (Config.get().debug) {
                Log.error("Hashed seed [" + hashedSeedData.getHashedSeed() + "]");
            }
        }
    }
}
