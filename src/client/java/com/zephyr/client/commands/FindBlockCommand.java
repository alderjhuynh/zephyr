package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Port of {@code cfindblock}. Finds nearest block matching predicate by scanning loaded chunks.
 */
public final class FindBlockCommand extends Command {
    public static final FindBlockCommand INSTANCE = new FindBlockCommand();

    private FindBlockCommand() {
        super("findblock", "Find nearest block: .z findblock <block> (alias: cfindblock)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return BuiltInRegistries.BLOCK.keySet().stream().map(Identifier::toString).toList();
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            CommandManager.sendMessage("You must be in a world");
            return;
        }
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z findblock <block>");
            return;
        }
        String blockName = args[0].trim();
        Identifier id = blockName.contains(":") ? Identifier.tryParse(blockName) : Identifier.tryParse("minecraft:" + blockName);
        if (id == null) { CommandManager.sendMessage("Invalid block: " + blockName); return; }
        Block target = BuiltInRegistries.BLOCK.getValue(id);
        if (target == Blocks.AIR && !id.toString().equals("minecraft:air")) {
            CommandManager.sendMessage("Unknown block: " + blockName);
            return;
        }
        CommandManager.sendMessage("Searching for " + id + "...");
        Vec3 eye = mc.player.getEyePosition(0);
        BlockPos playerPos = mc.player.blockPosition();
        int radius = Minecraft.getInstance().options.renderDistance().get() * 16;
        radius = Math.min(radius, 128);
        BlockPos closest = null;
        double best = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -64; y <= 64; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = playerPos.offset(x, y, z);
                    if (!mc.level.hasChunkAt(p)) continue;
                    if (mc.level.getBlockState(p).is(target)) {
                        double d = p.distToCenterSqr(eye);
                        if (d < best) { best = d; closest = p.immutable(); }
                    }
                }
            }
        }
        if (closest == null) {
            CommandManager.sendMessage("No " + id + " found within " + radius + " blocks");
        } else {
            double dist = Math.sqrt(best);
            CommandManager.sendMessage("Found " + id + " at " + closest.toShortString() + " (" + String.format("%.1f", dist) + "m)");
            // glow hint
            CommandManager.sendMessage("Use .z glow block " + closest.getX() + " " + closest.getY() + " " + closest.getZ() + " to highlight");
        }
    }
}
