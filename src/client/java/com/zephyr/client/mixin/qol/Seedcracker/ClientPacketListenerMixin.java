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

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Shadow
    private ClientLevel level;

    @Inject(method = "handleLevelChunkWithLight", at = @At(value = "TAIL"))
    private void onChunkData(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        int chunkX = packet.getX();
        int chunkZ = packet.getZ();
        FinderQueue.get().onChunkData(this.level, new ChunkPos(chunkX, chunkZ));
    }

    @Inject(method = "handleLogin", at = @At(value = "TAIL"))
    public void onGameJoin(ClientboundLoginPacket packet, CallbackInfo ci) {
        newDimension(new HashedSeedData(packet.commonPlayerSpawnInfo().seed()), false);
    }

    @Inject(method = "handleRespawn", at = @At(value = "TAIL"))
    public void onPlayerRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        newDimension(new HashedSeedData(packet.commonPlayerSpawnInfo().seed()), true);
    }

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
