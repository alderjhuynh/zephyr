package com.zephyr.client.commands;

import com.zephyr.client.module.bots.pathing.Pathing;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * The {@code .z path <...>} command: sets the {@link Pathing} module's
 * destination so it walks there using A* pathfinding. Coordinates are accepted
 * as three numbers; {@code stop} halts the current walk. Registered as a
 * singleton via {@link CommandManager}.
 */
public final class PathCommand extends Command {

    public static final PathCommand INSTANCE = new PathCommand();

    private PathCommand() {
        super("path", "Walks to coordinates via A*: .z path <x> <y> <z> | .z path stop");
    }

    /**
     * Offers tab-completion for the {@code stop} subcommand.
     *
     * @param args the arguments parsed up to the cursor
     * @return the candidate completions for the current argument position
     */
    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            return List.of("stop");
        }
        return List.of();
    }

    /**
     * Parses the coordinates and hands them to the {@link Pathing} module, or
     * stops the current walk for the {@code stop} subcommand.
     *
     * @param args the positional arguments after {@code .z path}
     */
    @Override
    public void execute(String[] args) {
        Minecraft client = Minecraft.getInstance();
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z path <x> <y> <z> | .z path stop");
            return;
        }
        if (args[0].equalsIgnoreCase("stop")) {
            Pathing.INSTANCE.setEnabled(false);
            CommandManager.sendMessage("Pathing stopped");
            return;
        }
        if (client.player == null || client.level == null) {
            CommandManager.sendMessage("You need to be in a world to use .z path");
            return;
        }
        if (args.length < 3) {
            CommandManager.sendMessage("Usage: .z path <x> <y> <z> | .z path stop");
            return;
        }
        double x;
        double y;
        double z;
        try {
            x = Double.parseDouble(args[0]);
            y = Double.parseDouble(args[1]);
            z = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            CommandManager.sendMessage("Usage: .z path <x> <y> <z> | .z path stop");
            return;
        }
        BlockPos target = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        Pathing.INSTANCE.startPath(target);
        CommandManager.sendMessage("Pathing to " + target.toShortString());
    }
}