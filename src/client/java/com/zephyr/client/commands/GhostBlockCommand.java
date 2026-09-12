package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * Port of {@code cghostblock}. Sets client-side ghost blocks (not sent to server).
 */
public final class GhostBlockCommand extends Command {
    public static final GhostBlockCommand INSTANCE = new GhostBlockCommand();

    private GhostBlockCommand() {
        super("ghostblock", "Ghost blocks client-side: .z ghostblock <set <x> <y> <z> <block>|fill <x1> <y1> <z1> <x2> <y2> <z2> <block> [replace <filter>]> (alias: cghostblock)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("set", "fill");
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { CommandManager.sendMessage("You must be in a world"); return; }
        if (args.length == 0) { CommandManager.sendMessage("Usage: .z ghostblock <set|fill> ..."); return; }
        String sub = args[0].toLowerCase();
        if (sub.equals("set")) {
            if (args.length < 5) { CommandManager.sendMessage("Usage: .z ghostblock set <x> <y> <z> <block>"); return; }
            try {
                BlockPos pos = new BlockPos(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                BlockState state = parseState(String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length)));
                if (state == null) { CommandManager.sendMessage("Unknown block: " + args[4]); return; }
                if (!mc.level.hasChunkAt(pos)) { CommandManager.sendMessage("Chunk not loaded"); return; }
                boolean ok = mc.level.setBlock(pos, state, 18);
                CommandManager.sendMessage(ok ? "Set ghost block at " + pos.toShortString() : "Failed to set ghost block");
            } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid coordinates"); }
        } else if (sub.equals("fill")) {
            if (args.length < 8) { CommandManager.sendMessage("Usage: .z ghostblock fill <x1> <y1> <z1> <x2> <y2> <z2> <block> [replace <filter>]"); return; }
            try {
                BlockPos from = new BlockPos(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                BlockPos to = new BlockPos(Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]));
                String blockStr = args[7];
                String filterStr = null;
                if (args.length > 9 && args[8].equalsIgnoreCase("replace")) filterStr = args[9];
                BlockState state = parseState(blockStr);
                if (state == null) { CommandManager.sendMessage("Unknown block: " + blockStr); return; }
                BlockState filter = filterStr != null ? parseState(filterStr) : null;
                if (filterStr != null && filter == null) { CommandManager.sendMessage("Unknown filter block: " + filterStr); return; }
                BoundingBox box = BoundingBox.fromCorners(from, to);
                int count = 0;
                for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                    if (filter != null && !mc.level.getBlockState(p).is(filter.getBlock())) continue;
                    if (mc.level.setBlock(p, state, 18)) count++;
                }
                if (count == 0) CommandManager.sendMessage("Failed: no blocks filled");
                else CommandManager.sendMessage("Filled " + count + " ghost blocks");
            } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid coordinates"); }
        } else {
            CommandManager.sendMessage("Usage: .z ghostblock <set|fill> ...");
        }
    }

    private BlockState parseState(String s) {
        // strip block state properties [prop=val]
        String base = s.split("\\[")[0].trim();
        Identifier id = base.contains(":") ? Identifier.tryParse(base) : Identifier.tryParse("minecraft:" + base);
        if (id == null) return null;
        var block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == Blocks.AIR && !id.toString().equals("minecraft:air")) return null;
        // ignore properties for simplicity
        return block.defaultBlockState();
    }
}
