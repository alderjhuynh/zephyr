package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

/**
 * Port of {@code cpos}. Converts coordinates between Overworld and Nether.
 */
public final class PosCommand extends Command {
    public static final PosCommand INSTANCE = new PosCommand();

    private PosCommand() {
        super("pos", "Convert coords: .z pos [to|from <overworld|nether|end>] [<x> <y> <z>] (alias: cpos)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("to", "from");
        if (args.length == 2 && (args[0].equalsIgnoreCase("to") || args[0].equalsIgnoreCase("from"))) return List.of("overworld","nether","end");
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { CommandManager.sendMessage("You must be in a world"); return; }
        // parse syntax: [to <dim>] [from <dim>] [<x> <y> <z>]
        // simplified: detect args
        ResourceKey<Level> sourceDim = null, targetDim = null;
        BlockPos pos = null;
        int i = 0;
        while (i < args.length) {
            String t = args[i].toLowerCase(Locale.ROOT);
            if (t.equals("to") && i+1 < args.length) {
                targetDim = parseDim(args[i+1]);
                if (targetDim == null) { CommandManager.sendMessage("Unknown dimension: " + args[i+1]); return; }
                i+=2;
            } else if (t.equals("from") && i+1 < args.length) {
                sourceDim = parseDim(args[i+1]);
                if (sourceDim == null) { CommandManager.sendMessage("Unknown dimension: " + args[i+1]); return; }
                i+=2;
            } else {
                // remaining should be coordinates
                if (args.length - i >= 3) {
                    try {
                        int x = Integer.parseInt(args[i]); int y = Integer.parseInt(args[i+1]); int z = Integer.parseInt(args[i+2]);
                        pos = new BlockPos(x,y,z);
                        i+=3;
                        break;
                    } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid coordinates: " + args[i]); return; }
                } else {
                    // no pos given, use player pos
                    pos = mc.player.blockPosition();
                    break;
                }
            }
        }
        if (pos == null) pos = mc.player.blockPosition();
        ResourceKey<Level> current = mc.player.level().dimension();
        if (sourceDim == null && targetDim == null) {
            sourceDim = current;
            targetDim = getOpposite(current);
        } else if (targetDim == null) {
            targetDim = getOpposite(sourceDim);
        } else if (sourceDim == null) {
            sourceDim = getOpposite(targetDim);
        }
        double scale = (double)getScale(sourceDim)/getScale(targetDim);
        BlockPos targetPos = BlockPos.containing(pos.getX()*scale, pos.getY(), pos.getZ()*scale);
        String srcName = dimName(sourceDim);
        String tgtName = dimName(targetDim);
        CommandManager.sendMessage(pos.toShortString() + " in " + srcName + " -> " + targetPos.toShortString() + " in " + tgtName);
    }

    private ResourceKey<Level> parseDim(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "overworld", "minecraft:overworld" -> Level.OVERWORLD;
            case "nether", "minecraft:the_nether", "the_nether" -> Level.NETHER;
            case "end", "minecraft:the_end", "the_end" -> Level.END;
            default -> null;
        };
    }

    private ResourceKey<Level> getOpposite(ResourceKey<Level> l) {
        if (l == Level.NETHER) return Level.OVERWORLD;
        return Level.NETHER;
    }

    private int getScale(ResourceKey<Level> l) {
        if (l == Level.NETHER) return 8;
        return 1;
    }

    private String dimName(ResourceKey<Level> l) {
        if (l == Level.OVERWORLD) return "Overworld";
        if (l == Level.NETHER) return "Nether";
        if (l == Level.END) return "The End";
        return l.identifier().toString();
    }
}
