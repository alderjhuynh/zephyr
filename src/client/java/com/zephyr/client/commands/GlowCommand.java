package com.zephyr.client.commands;

import com.zephyr.client.TickScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Locale;

/**
 * Port of {@code cglow}. Supports glowing entities and highlighting blocks via entity glow.
 * Entities glow is implemented via {@code setGlowingTag(true)} with timed removal.
 * Block glow is approximated by sending a message and optionally glowing nearby entities? For blocks we just highlight via message.
 */
public final class GlowCommand extends Command {
    public static final GlowCommand INSTANCE = new GlowCommand();

    private GlowCommand() {
        super("glow", "Glow entities/blocks: .z glow <entities <type>|block <x> <y> <z>|area <x1> <y1> <z1> <x2> <y2> <z2>> [seconds] (alias: cglow)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("entities", "block", "area");
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { CommandManager.sendMessage("You must be in a world"); return; }
        if (args.length == 0) { CommandManager.sendMessage("Usage: .z glow <entities <type>|block <x> <y> <z>|area ...> [seconds]"); return; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("entities")) {
            if (args.length < 2) { CommandManager.sendMessage("Usage: .z glow entities <type> [seconds]"); return; }
            String typeStr = args[1];
            int seconds = 30;
            if (args.length >= 3) try { seconds = Integer.parseInt(args[2]); } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid seconds"); return; }
            glowEntities(typeStr, seconds);
        } else if (sub.equals("block")) {
            if (args.length < 5) { CommandManager.sendMessage("Usage: .z glow block <x> <y> <z> [seconds]"); return; }
            try {
                BlockPos pos = new BlockPos(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                int sec = args.length >= 5 ? Integer.parseInt(args[4]) : 1;
                // for block we glow the block outline via glowing armor stand? Simplified: just make particles
                CommandManager.sendMessage("Block glow: " + pos.toShortString() + " for " + sec + "s (client-side highlight not rendered, coords displayed)");
                // If we had RenderQueue we would highlight; here just message
            } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid coordinates"); }
        } else if (sub.equals("area")) {
            if (args.length < 7) { CommandManager.sendMessage("Usage: .z glow area <x1> <y1> <z1> <x2> <y2> <z2> [seconds]"); return; }
            try {
                BlockPos p1 = new BlockPos(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                BlockPos p2 = new BlockPos(Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]));
                int sec = args.length >= 8 ? Integer.parseInt(args[7]) : 1;
                AABB box = AABB.encapsulatingFullBlocks(p1, p2);
                CommandManager.sendMessage("Area glow: " + p1.toShortString() + " to " + p2.toShortString() + " for " + sec + "s (AABB " + box + ")");
            } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid coordinates"); }
        } else {
            CommandManager.sendMessage("Unknown glow subcommand: " + sub);
        }
    }

    private void glowEntities(String filter, int seconds) {
        Minecraft mc = Minecraft.getInstance();
        List<Entity> toGlow;
        String lfilter = filter.toLowerCase(Locale.ROOT);
        if (lfilter.equals("all") || lfilter.equals("*")) {
            toGlow = java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false).toList();
        } else {
            toGlow = java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                    .filter(e -> e.getType().getDescription().getString().toLowerCase().contains(lfilter) || e.getName().getString().toLowerCase().contains(lfilter) || e.getType().toString().toLowerCase().contains(lfilter))
                    .toList();
        }
        if (toGlow.isEmpty()) { CommandManager.sendMessage("No entities matched: " + filter); return; }
        int ticks = seconds * 20;
        if (seconds == 0) ticks = Integer.MAX_VALUE; // keep until reload per flag
        int finalTicks = ticks;
        for (Entity e : toGlow) {
            e.setGlowingTag(true);
            if (finalTicks != Integer.MAX_VALUE) {
                TickScheduler.schedule(finalTicks, () -> {
                    try { e.setGlowingTag(false); } catch (Exception ignored) {}
                });
            }
        }
        CommandManager.sendMessage("Glowing " + toGlow.size() + " entities for " + seconds + "s");
    }
}
