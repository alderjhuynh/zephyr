package com.zephyr.client.commands;

import com.zephyr.client.module.bots.pathing.AStarPathfinder;
import com.zephyr.client.module.bots.pathing.BlockLocator;
import com.zephyr.client.module.bots.pathing.Pathing;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// 1.21.1 port: Identifier -> ResourceLocation, BLOCK.getValue -> BLOCK.get.
public final class PathCommand extends Command {

    public static final PathCommand INSTANCE = new PathCommand();

    private PathCommand() {
        super("path", "Walks to coordinates via A*: .z path <x> <y> <z> [destructive] | .z path stop | .z path task mine <block>");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            return List.of("stop", "destructive", "task");
        }
        if (args[0].equalsIgnoreCase("task") && args.length == 2) {
            return List.of("mine");
        }
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft client = Minecraft.getInstance();
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z path <x> <y> <z> [destructive] | .z path stop | .z path task mine <block>");
            return;
        }
        if (args[0].equalsIgnoreCase("stop")) {
            Pathing.INSTANCE.setEnabled(false);
            CommandManager.sendMessage("Pathing stopped");
            return;
        }
        if (args[0].equalsIgnoreCase("task")) {
            task(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        if (client.player == null || client.level == null) {
            CommandManager.sendMessage("You need to be in a world to use .z path");
            return;
        }

        boolean destructive = false;
        List<String> coords = new ArrayList<>();
        for (String arg : args) {
            if (arg.equalsIgnoreCase("destructive")) {
                destructive = true;
            } else {
                coords.add(arg);
            }
        }
        if (coords.size() != 3) {
            CommandManager.sendMessage("Usage: .z path <x> <y> <z> [destructive] | .z path stop | .z path task mine <block>");
            return;
        }

        double x;
        double y;
        double z;
        try {
            x = Double.parseDouble(coords.get(0));
            y = Double.parseDouble(coords.get(1));
            z = Double.parseDouble(coords.get(2));
        } catch (NumberFormatException e) {
            CommandManager.sendMessage("Usage: .z path <x> <y> <z> [destructive] | .z path stop | .z path task mine <block>");
            return;
        }
        BlockPos target = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        AStarPathfinder.PathResult result = Pathing.INSTANCE.startPath(target, destructive);

        StringBuilder msg = new StringBuilder("Pathing to ").append(target.toShortString());
        if (destructive && result != null) {
            if (result.path().isEmpty()) {
                msg.append(" (destructive: no path found)");
            } else {
                msg.append(String.format(" (destructive: route cost %.1f, %d to mine, %d to place)",
                        result.cost(), result.blocksToMine().size(), result.blocksToPlace().size()));
            }
        }
        CommandManager.sendMessage(msg.toString());
    }

    private void task(String[] args) {
        if (args.length < 2) {
            CommandManager.sendMessage("Usage: .z path task mine <block>");
            return;
        }
        if (!args[0].equalsIgnoreCase("mine")) {
            CommandManager.sendMessage("Usage: .z path task mine <block>");
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            CommandManager.sendMessage("You need to be in a world to use .z path task");
            return;
        }

        String blockName = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        ResourceLocation id = blockName.contains(":")
                ? ResourceLocation.tryParse(blockName)
                : ResourceLocation.tryParse("minecraft:" + blockName);
        if (id == null) {
            CommandManager.sendMessage("Invalid block: " + blockName);
            return;
        }
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == Blocks.AIR) {
            CommandManager.sendMessage("Unknown block: " + blockName);
            return;
        }

        BlockPos found = BlockLocator.findNearest(client, block);
        if (found == null) {
            CommandManager.sendMessage("No " + blockName + " found nearby");
            return;
        }

        AStarPathfinder.PathResult result = Pathing.INSTANCE.startTask(found, block, id.toString());
        if (result == null) {
            CommandManager.sendMessage("Task: could not start pathing to " + id);
            return;
        }
        if (result.path().isEmpty()) {
            Pathing.INSTANCE.setEnabled(false);
            CommandManager.sendMessage("Task: no path to " + id + " at " + found.toShortString());
            return;
        }
        CommandManager.sendMessage("Task: pathing to nearest " + id + " at " + found.toShortString() + " to mine it");
    }
}
