package com.zephyr.client.commands;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.UUID;

/**
 * Port of {@code cgetdata}. Supports {@code .z getdata entity <uuid|selector>} and {@code block <x> <y> <z>}.
 */
public final class GetDataCommand extends Command {
    public static final GetDataCommand INSTANCE = new GetDataCommand();

    private GetDataCommand() {
        super("getdata", "Get NBT data: .z getdata <entity <uuid>|block <x> <y> <z>> [path] (alias: cgetdata)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("entity", "block");
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { CommandManager.sendMessage("You must be in a world"); return; }
        if (args.length == 0) { CommandManager.sendMessage("Usage: .z getdata <entity <uuid>|block <x> <y> <z>> [path]"); return; }
        String sub = args[0].toLowerCase();
        if (sub.equals("entity")) {
            if (args.length < 2) { CommandManager.sendMessage("Usage: .z getdata entity <uuid> [path]"); return; }
            String uuidStr = args[1];
            String path = args.length > 2 ? String.join(".", java.util.Arrays.copyOfRange(args, 2, args.length)) : null;
            handleEntity(uuidStr, path);
        } else if (sub.equals("block")) {
            if (args.length < 4) { CommandManager.sendMessage("Usage: .z getdata block <x> <y> <z> [path]"); return; }
            try {
                int x = Integer.parseInt(args[1]); int y = Integer.parseInt(args[2]); int z = Integer.parseInt(args[3]);
                String path = args.length > 4 ? String.join(".", java.util.Arrays.copyOfRange(args, 4, args.length)) : null;
                handleBlock(new BlockPos(x, y, z), path);
            } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid coordinates"); }
        } else {
            CommandManager.sendMessage("Usage: .z getdata <entity <uuid>|block <x> <y> <z>> [path]");
        }
    }

    private void handleEntity(String uuidStr, String path) {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = null;
        try {
            UUID uuid = UUID.fromString(uuidStr);
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e.getUUID().equals(uuid)) { entity = e; break; }
            }
            if (entity == null) {
                // also search via getEntities
                for (Entity e : mc.level.getEntities((Entity) null, mc.player.getBoundingBox().inflate(512), e -> e.getUUID().equals(uuid))) {
                    entity = e; break;
                }
            }
        } catch (IllegalArgumentException e) {
            // treat as name
            String lname = uuidStr.toLowerCase();
            for (Entity en : mc.level.entitiesForRendering()) {
                if (en.getName().getString().toLowerCase().contains(lname)) { entity = en; break; }
            }
        }
        if (entity == null) { CommandManager.sendMessage("Entity not found: " + uuidStr); return; }
        try {
            CompoundTag tag;
            try (var collector = new net.minecraft.util.ProblemReporter.ScopedCollector(LogUtils.getLogger())) {
                var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(collector, mc.level.registryAccess());
                entity.saveWithoutId(output);
                tag = output.buildResult();
            }
            if (path != null && !path.isEmpty()) {
                Tag sub = getPath(tag, path);
                if (sub == null) { CommandManager.sendMessage("Path not found: " + path); return; }
                CommandManager.sendMessage(entity.getName().getString() + " path " + path + ": " + sub.toString());
            } else {
                CommandManager.sendMessage(entity.getName().getString() + " data: " + tag.toString());
            }
        } catch (Exception e) {
            CommandManager.sendMessage("Failed to get data: " + e.getMessage());
        }
    }

    private void handleBlock(BlockPos pos, String path) {
        Minecraft mc = Minecraft.getInstance();
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (be == null) { CommandManager.sendMessage("No block entity at " + pos.toShortString()); return; }
        try {
            CompoundTag tag = be.saveWithoutMetadata(mc.level.registryAccess());
            if (path != null && !path.isEmpty()) {
                Tag sub = getPath(tag, path);
                if (sub == null) { CommandManager.sendMessage("Path not found: " + path); return; }
                CommandManager.sendMessage("Block " + pos.toShortString() + " path " + path + ": " + sub.toString());
            } else {
                CommandManager.sendMessage("Block " + pos.toShortString() + " data: " + tag.toString());
            }
        } catch (Exception e) {
            CommandManager.sendMessage("Failed: " + e.getMessage());
        }
    }

    private Tag getPath(CompoundTag root, String path) {
        String[] parts = path.split("\\.");
        Tag cur = root;
        for (String p : parts) {
            if (cur instanceof CompoundTag ct) {
                if (!ct.contains(p)) return null;
                cur = ct.get(p);
            } else return null;
        }
        return cur;
    }
}
