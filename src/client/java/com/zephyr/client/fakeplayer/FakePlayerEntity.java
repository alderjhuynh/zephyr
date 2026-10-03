package com.zephyr.client.fakeplayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// Backport of 26.3's FakePlayerEntity (carpet EntityPlayerMPFake port).
// 1.21.1 differences: no ResolvableProfile async fetch (spawn synchronously
// with an offline profile), player data loads via NBT CompoundTag (no
// ValueInput/ProblemReporter), teleportTo has no trailing boolean, there is
// no TeleportTransition class or ClientboundEntityPositionSyncPacket, and
// LivingEntity.kill() takes no arguments.
@SuppressWarnings("EntityConstructor")
public class FakePlayerEntity extends ServerPlayer {
    private static final Set<String> spawning = new HashSet<>();

    public Runnable fixStartingPosition = () -> {};
    public boolean isAShadow;

    // Action pack — lightweight port of Carpet's EntityPlayerActionPack
    public final FakePlayerActionPack actionPack = new FakePlayerActionPack(this);

    public static boolean createFake(String username, MinecraftServer server, Vec3 pos, double yaw, double pitch, ResourceKey<Level> dimensionId, GameType gamemode, boolean flying) {
        ServerLevel worldIn = server.getLevel(dimensionId);
        if (worldIn == null) return false;

        UUID uuid = OldUsersConverter.convertMobOwnerIfNecessary(server, username);
        if (uuid == null) {
            // allow offline like carpet's allowSpawningOfflinePlayers — in Zephyr singleplayer we always allow offline
            uuid = UUIDUtil.createOfflinePlayerUUID(username);
        }
        if (uuid == null) return false;
        GameProfile gameprofile = new GameProfile(uuid, username);

        String name = gameprofile.getName();
        if (spawning.contains(name)) return false;
        spawning.add(name);
        try {
            FakePlayerEntity instance = new FakePlayerEntity(server, worldIn, gameprofile, ClientInformation.createDefault(), false);
            instance.fixStartingPosition = () -> instance.moveTo(pos.x, pos.y, pos.z, (float) yaw, (float) pitch);
            // Ensure starting position before placeNewPlayer; mixin fix is inlined here
            instance.moveTo(pos.x, pos.y, pos.z, (float) yaw, (float) pitch);
            server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), instance, new CommonListenerCookie(gameprofile, 0, instance.clientInformation(), false));
            // Run the fix after place (mirrors carpet PlayerList_fakePlayersMixin:37)
            instance.fixStartingPosition.run();
            loadPlayerData(instance);
            instance.stopRiding();
            instance.teleportTo(worldIn, pos.x, pos.y, pos.z, Set.of(), (float) yaw, (float) pitch);
            instance.setHealth(20.0F);
            instance.unsetRemoved();
            if (instance.getAttribute(Attributes.STEP_HEIGHT) != null) {
                instance.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);
            }
            instance.gameMode.changeGameModeForPlayer(gamemode);
            server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(instance, (byte) (instance.yHeadRot * 256 / 360)), dimensionId);
            server.getPlayerList().broadcastAll(new ClientboundTeleportEntityPacket(instance), dimensionId);
            instance.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f);
            instance.getAbilities().flying = flying;
        } finally {
            spawning.remove(name);
        }
        return true;
    }

    private static void loadPlayerData(FakePlayerEntity player) {
        Optional<CompoundTag> tag = player.level().getServer().getPlayerList().load(player);
        tag.ifPresent(player::load);
    }

    public static FakePlayerEntity createShadow(MinecraftServer server, ServerPlayer player) {
        player.connection.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
        ServerLevel worldIn = player.serverLevel();
        GameProfile gameprofile = player.getGameProfile();
        FakePlayerEntity playerShadow = new FakePlayerEntity(server, worldIn, gameprofile, player.clientInformation(), true);
        playerShadow.setChatSession(player.getChatSession());
        server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), playerShadow, new CommonListenerCookie(gameprofile, 0, player.clientInformation(), true));
        loadPlayerData(playerShadow);
        playerShadow.setHealth(player.getHealth());
        playerShadow.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        playerShadow.gameMode.changeGameModeForPlayer(player.gameMode.getGameModeForPlayer());
        playerShadow.actionPack.copyFrom(player instanceof FakePlayerEntity f ? f.actionPack : null);
        // carry over original player's pending actions if it had a fake actionpack via Zephyr? For now copy empty.
        if (playerShadow.getAttribute(Attributes.STEP_HEIGHT) != null) {
            playerShadow.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);
        }
        playerShadow.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, player.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
        server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(playerShadow, (byte) (player.yHeadRot * 256 / 360)), playerShadow.level().dimension());
        server.getPlayerList().broadcastAll(new ClientboundTeleportEntityPacket(playerShadow), playerShadow.level().dimension());
        playerShadow.getAbilities().flying = player.getAbilities().flying;
        return playerShadow;
    }

    public static FakePlayerEntity respawnFake(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation cli) {
        return new FakePlayerEntity(server, level, profile, cli, false);
    }

    public static boolean isSpawningPlayer(String username) {
        return spawning.contains(username);
    }

    private FakePlayerEntity(MinecraftServer server, ServerLevel worldIn, GameProfile profile, ClientInformation cli, boolean shadow) {
        super(server, worldIn, profile, cli);
        this.isAShadow = shadow;
    }

    @Override
    public void onEquipItem(final EquipmentSlot slot, final ItemStack previous, final ItemStack stack) {
        if (!isUsingItem()) super.onEquipItem(slot, previous, stack);
    }

    @Override
    public void kill() {
        kill(Component.literal("Killed"));
    }

    public void kill(Component reason) {
        shakeOff();
        if (reason.getContents() instanceof TranslatableContents text && text.getKey().equals("multiplayer.disconnect.duplicate_login")) {
            this.connection.onDisconnect(new DisconnectionDetails(reason));
        } else {
            this.level().getServer().execute(() -> this.connection.disconnect(reason));
        }
    }

    @Override
    public void tick() {
        // tick action pack before super.tick (mirrors ServerPlayer_actionPackMixin tick HEAD)
        actionPack.onUpdate();
        if (this.level().getServer().getTickCount() % 10 == 0) {
            this.connection.resetPosition();
            ((net.minecraft.server.level.ServerChunkCache) this.level().getChunkSource()).move(this);
        }
        try {
            super.tick();
            this.doTick();
        } catch (NullPointerException ignored) {
        }
    }

    @Override
    public boolean startRiding(Entity entityToRide, boolean force) {
        if (super.startRiding(entityToRide, force)) {
            if (entityToRide instanceof Boat) {
                this.yRotO = entityToRide.getYRot();
                this.setYRot(entityToRide.getYRot());
                this.setYHeadRot(entityToRide.getYRot());
            }
            return true;
        } else {
            return false;
        }
    }

    private void shakeOff() {
        if (getVehicle() instanceof Player) stopRiding();
        for (Entity passenger : getIndirectPassengers()) {
            if (passenger instanceof Player) passenger.stopRiding();
        }
    }

    @Override
    public void die(DamageSource cause) {
        shakeOff();
        super.die(cause);
        setHealth(20);
        this.foodData = new FoodData();
        kill(this.getCombatTracker().getDeathMessage());
    }

    @Override
    public String getIpAddress() {
        return "127.0.0.1";
    }

    @Override
    public boolean allowsListing() {
        return false;
    }
}
