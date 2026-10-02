package com.zephyr.client.cornerstone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.zephyr.client.commands.Command;
import com.zephyr.client.commands.CommandManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.permissions.Permissions;

/**
 * Zephyr {@code zcommand} port of cornerstone's {@code /cornerstone} Brigadier tree.
 *
 * <p>Usage (all via the Zephyr prefix, e.g. {@code .z cornerstone ...}):
 * <pre>
 * .z cornerstone select
 * .z cornerstone pos1 &lt;x&gt; &lt;y&gt; &lt;z&gt;
 * .z cornerstone pos2 &lt;x&gt; &lt;y&gt; &lt;z&gt;
 * .z cornerstone clear
 * .z cornerstone save &lt;name&gt; [air]
 * .z cornerstone run &lt;name&gt; [here | at &lt;x&gt; &lt;y&gt; &lt;z&gt;]
 * .z cornerstone list
 * .z cornerstone delete &lt;name&gt;
 * .z cornerstone cancel
 * </pre>
 * Coordinates accept {@code ~} relative syntax (e.g. {@code ~ ~1 ~-2}).
 */
public final class CornerstoneCommand extends Command {
    public static final CornerstoneCommand INSTANCE = new CornerstoneCommand();

    private CornerstoneCommand() {
        super("cornerstone",
                "Copy areas as setblock/fill commands: .z cornerstone <select|pos1|pos2|clear|save|run|list|delete|cancel> (alias: ccornerstone)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length <= 1) {
            return List.of("select", "pos1", "pos2", "clear", "save", "run", "list", "delete", "cancel");
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "save" -> {
                if (args.length == 2) yield new ArrayList<>(CornerstoneStore.names());
                if (args.length == 3) yield List.of("air");
                yield List.of();
            }
            case "run", "delete" -> {
                if (args.length == 2) yield new ArrayList<>(CornerstoneStore.names());
                if (sub.equals("run") && args.length == 3) yield List.of("here", "at");
                yield List.of();
            }
            default -> List.of();
        };
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            usage();
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        switch (sub) {
            case "select" -> toggleSelect(rest);
            case "clear" -> {
                CornerstoneSelection.clear();
                CommandManager.sendMessage("Selection cleared.");
            }
            case "pos1" -> setCorner(rest, true);
            case "pos2" -> setCorner(rest, false);
            case "save" -> save(rest);
            case "run" -> run(rest);
            case "list" -> list();
            case "delete" -> delete(rest);
            case "cancel" -> cancel();
            default -> usage();
        }
    }

    private void usage() {
        CommandManager.sendMessage("Usage: .z cornerstone <select|pos1 <x> <y> <z>|pos2 <x> <y> <z>|clear|save <name> [air]|run <name> [here|at <x> <y> <z>]|list|delete <name>|cancel>");
    }

    private void toggleSelect(String[] rest) {
        if (rest.length != 0) {
            CommandManager.sendMessage("Usage: .z cornerstone select");
            return;
        }
        boolean on = CornerstoneSelection.toggle();
        CommandManager.sendMessage(on ? "Selection mode ON (left click = corner 1, right click = corner 2)"
                : "Selection mode OFF");
    }

    private void setCorner(String[] rest, boolean first) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            CommandManager.sendMessage("You must be in a world");
            return;
        }
        if (rest.length != 3) {
            CommandManager.sendMessage("Usage: .z cornerstone " + (first ? "pos1" : "pos2") + " <x> <y> <z>");
            return;
        }
        BlockPos pos;
        try {
            pos = parseBlockPos(rest, mc.player.blockPosition());
        } catch (NumberFormatException e) {
            CommandManager.sendMessage("Invalid coordinates: use integers or ~-relative (e.g. ~ ~1 ~-2)");
            return;
        }
        if (first) {
            CornerstoneSelection.setFirst(pos);
        } else {
            CornerstoneSelection.setSecond(pos);
        }
        String msg = (first ? "Corner 1 set: " : "Corner 2 set: ") + pos.toShortString();
        Optional<BlockPos> other = first ? CornerstoneSelection.second() : CornerstoneSelection.first();
        if (other.isPresent()) {
            BlockPos a = pos;
            BlockPos b = other.get();
            long volume = (long) (Math.abs(a.getX() - b.getX()) + 1)
                    * (Math.abs(a.getY() - b.getY()) + 1)
                    * (Math.abs(a.getZ() - b.getZ()) + 1);
            msg += " (" + volume + " blocks)";
            if (volume > 1_000_000) {
                msg += " large region: saving may take a while.";
            }
        }
        CommandManager.sendMessage(msg);
    }

    private void save(String[] rest) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            CommandManager.sendMessage("You must be in a world");
            return;
        }
        if (rest.length < 1 || rest.length > 2) {
            CommandManager.sendMessage("Usage: .z cornerstone save <name> [air]");
            return;
        }
        String name = rest[0];
        boolean includeAir = rest.length == 2 && rest[1].equalsIgnoreCase("air");
        if (rest.length == 2 && !includeAir) {
            CommandManager.sendMessage("Usage: .z cornerstone save <name> [air]");
            return;
        }

        Optional<BlockPos> a = CornerstoneSelection.first();
        Optional<BlockPos> b = CornerstoneSelection.second();
        if (a.isEmpty() || b.isEmpty()) {
            CommandManager.sendMessage("Select both corners first (.z cornerstone select, then click, or use pos1/pos2)");
            return;
        }
        if (CornerstoneSaver.isBusy()) {
            CommandManager.sendMessage("A save is already in progress. Wait or run .z cornerstone cancel.");
            return;
        }
        try {
            if (!CornerstoneSaver.start(name, mc.level, a.get(), b.get(), includeAir)) {
                CommandManager.sendMessage("A save is already in progress.");
                return;
            }
        } catch (IllegalArgumentException e) {
            CommandManager.sendMessage(e.getMessage());
            return;
        }
        CommandManager.sendMessage("Saving '" + name + "' in the background.");
    }

    private void run(String[] rest) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            CommandManager.sendMessage("You must be in a world");
            return;
        }
        if (rest.length < 1) {
            CommandManager.sendMessage("Usage: .z cornerstone run <name> [here|at <x> <y> <z>]");
            return;
        }
        String name = rest[0];
        SavedRegion region = CornerstoneStore.get(name);
        if (region == null) {
            CommandManager.sendMessage("No save named '" + name + "'.");
            return;
        }
        if (CornerstoneRunner.isBusy()) {
            CommandManager.sendMessage("A run is already in progress");
            return;
        }
        if (!mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            CommandManager.sendMessage("You need operator permissions to run this command.");
            return;
        }

        BlockPos origin;
        if (rest.length == 1) {
            origin = new BlockPos(region.originX, region.originY, region.originZ);
        } else if (rest.length == 2 && rest[1].equalsIgnoreCase("here")) {
            origin = mc.player.blockPosition();
        } else if (rest.length == 5 && rest[1].equalsIgnoreCase("at")) {
            try {
                origin = parseBlockPos(new String[]{rest[2], rest[3], rest[4]}, mc.player.blockPosition());
            } catch (NumberFormatException e) {
                CommandManager.sendMessage("Invalid coordinates: use integers or ~-relative");
                return;
            }
        } else {
            CommandManager.sendMessage("Usage: .z cornerstone run <name> [here|at <x> <y> <z>]");
            return;
        }

        String prefix = "execute positioned %d.0 %d.0 %d.0 run "
                .formatted(origin.getX(), origin.getY(), origin.getZ());

        List<String> commands = new ArrayList<>(region.commands.size());
        for (String command : region.commands) {
            commands.add(prefix + command);
        }
        CornerstoneRunner.start(commands);

        int seconds = commands.size() / CornerstoneStore.commandsPerTick() / 20;
        CommandManager.sendMessage("Running '" + name + "' at " + origin.toShortString() + ": "
                + commands.size() + " commands (~" + seconds + "s).");
    }

    private void list() {
        List<String> names = CornerstoneStore.names();
        if (names.isEmpty()) {
            CommandManager.sendMessage("No saves yet.");
            return;
        }
        for (String name : names) {
            SavedRegion r = CornerstoneStore.get(name);
            CommandManager.sendMessage(" - " + name + " (" + r.sizeX + "x" + r.sizeY + "x"
                    + r.sizeZ + ", " + r.commands.size() + " commands)");
        }
    }

    private void delete(String[] rest) {
        if (rest.length != 1) {
            CommandManager.sendMessage("Usage: .z cornerstone delete <name>");
            return;
        }
        if (CornerstoneStore.remove(rest[0])) {
            CommandManager.sendMessage("Deleted '" + rest[0] + "'.");
        } else {
            CommandManager.sendMessage("No save named '" + rest[0] + "'.");
        }
    }

    private void cancel() {
        int saveCancelled = CornerstoneSaver.cancel();
        int dropped = CornerstoneRunner.cancel();
        if (saveCancelled > 0 && dropped == 0) {
            CommandManager.sendMessage("Save cancelled.");
        } else if (saveCancelled > 0) {
            CommandManager.sendMessage("Save cancelled; " + dropped + " run commands dropped.");
        } else {
            CommandManager.sendMessage("Cancelled; " + dropped + " commands dropped.");
        }
    }

    private static BlockPos parseBlockPos(String[] parts, BlockPos base) throws NumberFormatException {
        return new BlockPos(
                parseCoord(parts[0], base.getX()),
                parseCoord(parts[1], base.getY()),
                parseCoord(parts[2], base.getZ()));
    }

    private static int parseCoord(String s, int base) throws NumberFormatException {
        if (s.startsWith("~")) {
            if (s.length() == 1) return base;
            return base + Integer.parseInt(s.substring(1));
        }
        return Integer.parseInt(s);
    }
}
