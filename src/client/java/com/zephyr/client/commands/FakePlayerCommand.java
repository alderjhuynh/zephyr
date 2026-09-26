package com.zephyr.client.commands;

import com.zephyr.client.fakeplayer.FakePlayerActionPack;
import com.zephyr.client.fakeplayer.FakePlayerEntity;
import com.zephyr.client.fakeplayer.FakePlayerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Singleplayer-only port of {@code carpet.commands.PlayerCommand}.
 * Registered as {@code .z player} (and {@code .player} via CommandManager).
 * Gates all actions with {@code FakePlayerManager.isSingleplayer()} so joining
 * vanilla/multiplayer servers without carpet or zephyr on the server is always safe.
 */
public final class FakePlayerCommand extends Command {
    public static final FakePlayerCommand INSTANCE = new FakePlayerCommand();

    private FakePlayerCommand() {
        super("player", "Fake player (singleplayer only): .z player <name> spawn|kill|stop|... (alias: cplayer)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 0) return List.of();
        if (args.length == 1) {
            // first arg is player name
            List<String> out = new ArrayList<>();
            out.add("Steve");
            out.add("Alex");
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                mc.getConnection().getOnlinePlayers().forEach(p -> out.add(p.getProfile().name()));
            }
            // also add existing fake players from integrated server
            IntegratedServer srv = FakePlayerManager.getServer();
            if (srv != null) {
                srv.getPlayerList().getPlayers().forEach(p -> {
                    if (p instanceof FakePlayerEntity) out.add(p.getGameProfile().name());
                });
            }
            return out;
        }
        if (args.length == 2) {
            return List.of("spawn", "kill", "stop", "use", "attack", "jump", "sneak", "unsneak", "sprint", "unsprint",
                    "look", "turn", "move", "hotbar", "drop", "dropStack", "mount", "dismount", "shadow", "list");
        }
        // deeper suggestions
        String action = args[1].toLowerCase(Locale.ROOT);
        if (args.length == 3) {
            return switch (action) {
                case "spawn" -> List.of("at", "facing", "in");
                case "use", "attack", "jump" -> List.of("once", "continuous", "interval");
                case "drop", "dropstack" -> List.of("all", "mainhand", "offhand");
                case "look" -> List.of("north", "south", "east", "west", "up", "down", "at");
                case "turn" -> List.of("left", "right", "back");
                case "move" -> List.of("forward", "backward", "left", "right");
                case "hotbar" -> List.of("1","2","3","4","5","6","7","8","9");
                default -> List.of();
            };
        }
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        if (!FakePlayerManager.isSingleplayer()) {
            CommandManager.sendMessage("§cFake players only work in singleplayer (integrated server).");
            CommandManager.sendMessage("§7Joining multiplayer servers with this mod is still safe — this command is disabled there.");
            return;
        }
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z player <name> <spawn|kill|stop|use|attack|jump|sneak|unsneak|sprint|unsprint|look|turn|move|hotbar|drop|shadow|list>");
            CommandManager.sendMessage("  spawn [at <x> <y> <z>] [facing <yaw> <pitch>] [in <survival|creative|adventure|spectator>] [in <dimension>]");
            return;
        }
        String playerName = args[0];
        // special: list does not need player name? we support .z player list
        if (playerName.equalsIgnoreCase("list") && args.length == 1) {
            listFakePlayers();
            return;
        }
        if (args.length == 1) {
            CommandManager.sendMessage("Usage: .z player " + playerName + " <action>");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        String[] rest = java.util.Arrays.copyOfRange(args, 2, args.length);

        switch (action) {
            case "spawn" -> handleSpawn(playerName, rest);
            case "kill" -> handleKill(playerName);
            case "stop" -> handleManipulate(playerName, ap -> ap.stopAll());
            case "use" -> handleUseAttack(playerName, FakePlayerActionPack.ActionType.USE, rest);
            case "attack" -> handleUseAttack(playerName, FakePlayerActionPack.ActionType.ATTACK, rest);
            case "jump" -> handleUseAttack(playerName, FakePlayerActionPack.ActionType.JUMP, rest);
            case "drop" -> handleDrop(playerName, rest, false);
            case "dropstack" -> handleDrop(playerName, rest, true);
            case "sneak" -> handleManipulate(playerName, ap -> ap.setSneaking(true));
            case "unsneak" -> handleManipulate(playerName, ap -> ap.setSneaking(false));
            case "sprint" -> handleManipulate(playerName, ap -> ap.setSprinting(true));
            case "unsprint" -> handleManipulate(playerName, ap -> ap.setSprinting(false));
            case "look" -> handleLook(playerName, rest);
            case "turn" -> handleTurn(playerName, rest);
            case "move" -> handleMove(playerName, rest);
            case "hotbar" -> handleHotbar(playerName, rest);
            case "mount" -> handleManipulate(playerName, ap -> ap.mount(true));
            case "dismount" -> handleManipulate(playerName, ap -> ap.dismount());
            case "shadow" -> handleShadow(playerName);
            case "list" -> listFakePlayers();
            case "swapHands" -> handleManipulate(playerName, ap -> ap.start(FakePlayerActionPack.ActionType.SWAP_HANDS, FakePlayerActionPack.Action.once()));
            default -> CommandManager.sendMessage("Unknown action: " + action);
        }
    }

    // --- handlers ---

    private void listFakePlayers() {
        IntegratedServer server = FakePlayerManager.getServer();
        if (server == null) { CommandManager.sendMessage("Not in singleplayer"); return; }
        var fakes = server.getPlayerList().getPlayers().stream().filter(p -> p instanceof FakePlayerEntity).toList();
        if (fakes.isEmpty()) CommandManager.sendMessage("No fake players");
        else {
            CommandManager.sendMessage("Fake players (" + fakes.size() + "):");
            fakes.forEach(p -> CommandManager.sendMessage("- " + p.getGameProfile().name() + " @ " + p.blockPosition().toShortString() + " in " + p.level().dimension().identifier()));
        }
    }

    private void handleSpawn(String name, String[] rest) {
        if (FakePlayerManager.isSpawning(name)) { CommandManager.sendMessage("Player " + name + " is currently logging on"); return; }
        IntegratedServer server = FakePlayerManager.getServer();
        if (server == null) { CommandManager.sendMessage("Not in singleplayer"); return; }
        if (server.getPlayerList().getPlayerByName(name) != null) { CommandManager.sendMessage("Player " + name + " is already logged on"); return; }
        if (name.length() > 16) { CommandManager.sendMessage("Player name too long: " + name); return; }

        Minecraft mc = Minecraft.getInstance();
        Vec3 pos = mc.player != null ? mc.player.position() : new Vec3(0, 64, 0);
        float yaw = mc.player != null ? mc.player.getYRot() : 0;
        float pitch = mc.player != null ? mc.player.getXRot() : 0;
        ResourceKey<Level> dim = mc.player != null ? mc.player.level().dimension() : Level.OVERWORLD;
        GameType mode = GameType.CREATIVE;
        boolean flying = false;
        if (mc.player != null) {
            flying = mc.player.getAbilities().flying;
            // try to get host's gamemode from integrated server if possible
            try {
                String hostName = Minecraft.getInstance().getUser().getName();
                ServerPlayer host = server.getPlayerList().getPlayerByName(hostName);
                if (host != null) mode = host.gameMode.getGameModeForPlayer();
            } catch (Exception ignored) {}
        }
        // parse rest: at x y z, facing yaw pitch, in <gamemode>, in <dimension>
        for (int i = 0; i < rest.length; i++) {
            String tok = rest[i].toLowerCase(Locale.ROOT);
            switch (tok) {
                case "at" -> {
                    if (i + 3 < rest.length) {
                        try {
                            double x = parseCoord(rest[i+1], pos.x);
                            double y = parseCoord(rest[i+2], pos.y);
                            double z = parseCoord(rest[i+3], pos.z);
                            pos = new Vec3(x, y, z);
                            i += 3;
                        } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid position"); return; }
                    }
                }
                case "facing" -> {
                    if (i + 2 < rest.length) {
                        try {
                            yaw = Float.parseFloat(rest[i+1]);
                            pitch = Float.parseFloat(rest[i+2]);
                            i += 2;
                        } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid rotation"); return; }
                    }
                }
                case "in" -> {
                    if (i + 1 < rest.length) {
                        GameType gm = parseGameMode(rest[i+1]);
                        if (gm != null) {
                            mode = gm;
                            // handle spectator/creative flying overrides like carpet PlayerCommand:296
                            if (mode == GameType.SPECTATOR) flying = true;
                            else if (mode.isSurvival()) flying = false;
                            i += 1;
                        } else {
                            // try dimension
                            ResourceKey<Level> d = parseDimension(rest[i+1]);
                            if (d != null) { dim = d; i += 1; }
                            else { CommandManager.sendMessage("Unknown gamemode/dimension: " + rest[i+1]); return; }
                        }
                    }
                }
                default -> {
                    // allow bare gamemode without 'in'
                    GameType gm = parseGameMode(tok);
                    if (gm != null) {
                        mode = gm;
                        if (mode == GameType.SPECTATOR) flying = true;
                        else if (mode.isSurvival()) flying = false;
                    }
                }
            }
        }

        if (!Level.isInSpawnableBounds(BlockPos.containing(pos))) { CommandManager.sendMessage("Cannot place outside world"); return; }

        boolean success = FakePlayerEntity.createFake(name, server, pos, yaw, pitch, dim, mode, flying);
        if (!success) CommandManager.sendMessage("Failed to spawn " + name + " (offline profile?)");
        else CommandManager.sendMessage("Spawning fake player " + name + " at " + pos.toString() + " yaw " + yaw + " pitch " + pitch + " in " + dim.identifier() + " gamemode " + mode.getName());
    }

    private double parseCoord(String s, double base) {
        if (s.startsWith("~")) {
            if (s.length() == 1) return base;
            return base + Double.parseDouble(s.substring(1));
        }
        return Double.parseDouble(s);
    }

    private GameType parseGameMode(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "survival", "0", "s" -> GameType.SURVIVAL;
            case "creative", "1", "c" -> GameType.CREATIVE;
            case "adventure", "2", "a" -> GameType.ADVENTURE;
            case "spectator", "3", "sp" -> GameType.SPECTATOR;
            default -> null;
        };
    }

    private ResourceKey<Level> parseDimension(String s) {
        try {
            Identifier id = s.contains(":") ? Identifier.parse(s) : Identifier.parse("minecraft:" + s);
            return ResourceKey.create(Registries.DIMENSION, id);
        } catch (Exception e) { return null; }
    }

    private void handleKill(String name) {
        ServerPlayer p = FakePlayerManager.getPlayer(name);
        if (p == null) { CommandManager.sendMessage("Can only kill existing players / fake players"); return; }
        if (!(p instanceof FakePlayerEntity fake)) { CommandManager.sendMessage("Only fake players can be moved or killed"); return; }
        fake.kill(fake.level().getServer().getLevel(fake.level().dimension()));
        CommandManager.sendMessage("Killed " + name);
    }

    private void handleManipulate(String name, java.util.function.Consumer<FakePlayerActionPack> action) {
        ServerPlayer p = FakePlayerManager.getPlayer(name);
        if (p == null) { CommandManager.sendMessage("Can only manipulate existing players"); return; }
        if (!(p instanceof FakePlayerEntity fake)) {
            // Allow non-OP to control only fakes, like carpet PlayerCommand:69
            CommandManager.sendMessage("Can only manipulate fake players (or you control yourself in singleplayer)");
            // In singleplayer you are OP, so allow self? For simplicity deny.
            return;
        }
        action.accept(fake.actionPack);
        CommandManager.sendMessage("OK");
    }

    private void handleUseAttack(String name, FakePlayerActionPack.ActionType type, String[] rest) {
        String sub = rest.length > 0 ? rest[0].toLowerCase(Locale.ROOT) : "once";
        FakePlayerActionPack.Action act = switch (sub) {
            case "continuous" -> FakePlayerActionPack.Action.continuous();
            case "interval" -> {
                int ticks = 1;
                if (rest.length > 1) try { ticks = Integer.parseInt(rest[1]); } catch (NumberFormatException ignored) {}
                yield FakePlayerActionPack.Action.interval(ticks);
            }
            default -> FakePlayerActionPack.Action.once();
        };
        handleManipulate(name, ap -> ap.start(type, act));
    }

    private void handleDrop(String name, String[] rest, boolean dropAll) {
        String slotStr = rest.length > 0 ? rest[0].toLowerCase(Locale.ROOT) : "mainhand";
        int slot = switch (slotStr) {
            case "all" -> -2;
            case "mainhand" -> -1;
            case "offhand" -> 40;
            default -> {
                try { yield Integer.parseInt(slotStr); } catch (NumberFormatException e) { yield -1; }
            }
        };
        int finalSlot = slot;
        handleManipulate(name, ap -> ap.drop(finalSlot, dropAll));
    }

    private void handleHotbar(String name, String[] rest) {
        if (rest.length == 0) { CommandManager.sendMessage("Usage: .z player <name> hotbar <1-9>"); return; }
        try {
            int slot = Integer.parseInt(rest[0]);
            if (slot < 1 || slot > 9) { CommandManager.sendMessage("Slot must be 1-9"); return; }
            handleManipulate(name, ap -> ap.setSlot(slot));
        } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid slot"); }
    }

    private void handleLook(String name, String[] rest) {
        if (rest.length == 0) { CommandManager.sendMessage("Usage: .z player <name> look <north|south|east|west|up|down|at x y z|yaw pitch>"); return; }
        String sub = rest[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "north" -> handleManipulate(name, ap -> ap.look(Direction.NORTH));
            case "south" -> handleManipulate(name, ap -> ap.look(Direction.SOUTH));
            case "east" -> handleManipulate(name, ap -> ap.look(Direction.EAST));
            case "west" -> handleManipulate(name, ap -> ap.look(Direction.WEST));
            case "up" -> handleManipulate(name, ap -> ap.look(Direction.UP));
            case "down" -> handleManipulate(name, ap -> ap.look(Direction.DOWN));
            case "at" -> {
                if (rest.length < 4) { CommandManager.sendMessage("Usage: look at <x> <y> <z>"); return; }
                try {
                    double x = Double.parseDouble(rest[1]);
                    double y = Double.parseDouble(rest[2]);
                    double z = Double.parseDouble(rest[3]);
                    handleManipulate(name, ap -> ap.lookAt(new Vec3(x, y, z)));
                } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid coords"); }
            }
            default -> {
                // try yaw pitch
                if (rest.length >= 2) {
                    try {
                        float yaw = Float.parseFloat(rest[0]);
                        float pitch = Float.parseFloat(rest[1]);
                        handleManipulate(name, ap -> ap.look(yaw, pitch));
                    } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid look args"); }
                } else CommandManager.sendMessage("Unknown look direction: " + sub);
            }
        }
    }

    private void handleTurn(String name, String[] rest) {
        if (rest.length == 0) { CommandManager.sendMessage("Usage: .z player <name> turn <left|right|back|yaw pitch>"); return; }
        String sub = rest[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "left" -> handleManipulate(name, ap -> ap.turn(-90, 0));
            case "right" -> handleManipulate(name, ap -> ap.turn(90, 0));
            case "back" -> handleManipulate(name, ap -> ap.turn(180, 0));
            default -> {
                try {
                    float yaw = Float.parseFloat(rest[0]);
                    float pitch = rest.length > 1 ? Float.parseFloat(rest[1]) : 0;
                    handleManipulate(name, ap -> ap.turn(yaw, pitch));
                } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid turn args"); }
            }
        }
    }

    private void handleMove(String name, String[] rest) {
        if (rest.length == 0) {
            handleManipulate(name, ap -> ap.stopMovement());
            return;
        }
        String dir = rest[0].toLowerCase(Locale.ROOT);
        switch (dir) {
            case "forward" -> handleManipulate(name, ap -> ap.setForward(1));
            case "backward" -> handleManipulate(name, ap -> ap.setForward(-1));
            case "left" -> handleManipulate(name, ap -> ap.setStrafing(1));
            case "right" -> handleManipulate(name, ap -> ap.setStrafing(-1));
            case "stop" -> handleManipulate(name, ap -> ap.stopMovement());
            default -> CommandManager.sendMessage("Unknown move: " + dir + " (forward/backward/left/right/stop)");
        }
    }

    private void handleShadow(String name) {
        if (!FakePlayerManager.isSingleplayer()) { CommandManager.sendMessage("Shadow only in singleplayer"); return; }
        ServerPlayer p = FakePlayerManager.getPlayer(name);
        if (p == null) { CommandManager.sendMessage("Can only manipulate existing players"); return; }
        if (p instanceof FakePlayerEntity) { CommandManager.sendMessage("Cannot shadow fake players"); return; }
        IntegratedServer server = FakePlayerManager.getServer();
        if (server == null) return;
        FakePlayerEntity.createShadow(server, p);
        CommandManager.sendMessage("Shadowed " + name);
    }
}
