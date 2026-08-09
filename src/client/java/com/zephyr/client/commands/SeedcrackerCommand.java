package com.zephyr.client.commands;

import com.seedfinding.mccore.version.MCVersion;
import com.zephyr.client.module.qol.Seedcracker;
import com.zephyr.client.module.qol.seedcracker.config.Config;
import com.zephyr.client.module.qol.seedcracker.cracker.storage.DataStorage;
import com.zephyr.client.module.qol.seedcracker.finder.Finder;
import com.zephyr.client.module.qol.seedcracker.finder.ReloadFinders;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class SeedcrackerCommand extends Command {

    public static final SeedcrackerCommand INSTANCE = new SeedcrackerCommand();

    private static final String DATABASE_URL = "https://docs.google.com/spreadsheets/d/1tuQiE-0leW88em9OHbZnH-RFNhVqgoHhIt9WQbeqqWw/edit?usp=sharing";

    private SeedcrackerCommand() {
        super("seedcracker", "Controls the Seedcracker module: .z seedcracker <cracker|data|finder|render|version|database|gui>");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            return List.of("cracker", "data", "finder", "render", "version", "database", "gui");
        }
        if (args[0].equalsIgnoreCase("render")) {
            if (args.length == 2) return List.of("outlines");
            if (args.length == 3) return List.of("OFF", "ON", "XRAY");
        }
        if (args[0].equalsIgnoreCase("finder")) {
            if (args.length == 2) return List.of("type", "category", "reload");
            if (args[1].equalsIgnoreCase("type")) {
                if (args.length == 3) return finderTypeNames();
                if (args.length >= 4) return List.of("ON", "OFF");
            }
            if (args[1].equalsIgnoreCase("category")) {
                if (args.length == 3) return finderCategoryNames();
                if (args.length >= 4) return List.of("ON", "OFF");
            }
        }
        if (args[0].equalsIgnoreCase("data")) {
            if (args.length == 2) return List.of("clear", "bits", "restore");
        }
        if (args[0].equalsIgnoreCase("cracker")) {
            if (args.length == 2) return List.of("ON", "OFF", "debug");
            if (args.length == 3) return List.of("ON", "OFF");
        }
        if (args[0].equalsIgnoreCase("version")) {
            if (args.length == 2) return versionNames();
        }
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            CommandManager.sendMessage("Error: please enter a valid seedcracker command");
            return;
        }
        if (args[0].equalsIgnoreCase("render")) {
            render(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        if (args[0].equalsIgnoreCase("finder")) {
            finder(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        if (args[0].equalsIgnoreCase("data")) {
            data(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        if (args[0].equalsIgnoreCase("cracker")) {
            cracker(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        if (args[0].equalsIgnoreCase("version")) {
            version(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        if (args[0].equalsIgnoreCase("database")) {
            openDatabase();
            return;
        }
        if (args[0].equalsIgnoreCase("gui")) {
            CommandManager.sendMessage("The Seedcracker config screen is not available in this port; configure the module from the click-gui instead.");
            return;
        }
        CommandManager.sendMessage("Error: please enter a valid seedcracker command");
    }

    private void render(String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("outlines")) {
            CommandManager.sendMessage("Usage: .z seedcracker render outlines [OFF|ON|XRAY]");
            return;
        }
        if (args.length == 1) {
            CommandManager.sendMessage("Current render mode is set to [" + Config.get().render + "].");
            return;
        }
        Config.RenderType mode = parseRenderType(args[1]);
        if (mode == null) {
            CommandManager.sendMessage("Usage: .z seedcracker render outlines [OFF|ON|XRAY]");
            return;
        }
        Seedcracker.get().setRender(mode);
        CommandManager.sendMessage("Changed render mode to [" + Config.get().render + "].");
    }

    private void finder(String[] args) {
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z seedcracker finder <type|category|reload> ...");
            return;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            reloadFinders();
            return;
        }
        if (args[0].equalsIgnoreCase("type")) {
            finderType(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        if (args[0].equalsIgnoreCase("category")) {
            finderCategory(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        CommandManager.sendMessage("Usage: .z seedcracker finder <type|category|reload> ...");
    }

    private void finderType(String[] args) {
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z seedcracker finder type <TYPE> [ON|OFF]");
            return;
        }
        Finder.Type type = parseFinderType(args[0]);
        if (type == null) {
            CommandManager.sendMessage("Unknown finder type: " + args[0]);
            return;
        }
        if (args.length == 1) {
            printFinderType(type);
            return;
        }
        Boolean flag = parseFlag(args[1]);
        if (flag == null) {
            CommandManager.sendMessage("Usage: .z seedcracker finder type " + type + " [ON|OFF]");
            return;
        }
        Seedcracker.get().setFinderEnabled(type, flag);
        CommandManager.sendMessage("Finder " + finderName(type) + " has been set to [" + (flag ? "TRUE" : "FALSE") + "].");
    }

    private void finderCategory(String[] args) {
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z seedcracker finder category <CATEGORY> [ON|OFF]");
            return;
        }
        Finder.Category category = parseCategory(args[0]);
        if (category == null) {
            CommandManager.sendMessage("Unknown finder category: " + args[0]);
            return;
        }
        if (args.length == 1) {
            for (Finder.Type type : Finder.Type.getForCategory(category)) {
                printFinderType(type);
            }
            return;
        }
        Boolean flag = parseFlag(args[1]);
        if (flag == null) {
            CommandManager.sendMessage("Usage: .z seedcracker finder category " + category + " [ON|OFF]");
            return;
        }
        for (Finder.Type type : Finder.Type.getForCategory(category)) {
            Seedcracker.get().setFinderEnabled(type, flag);
        }
    }

    private void printFinderType(Finder.Type type) {
        String state = type.enabled.get() ? "TRUE" : "FALSE";
        CommandManager.sendMessage("Finder " + finderName(type) + " is set to [" + state + "].");
    }

    private void reloadFinders() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            CommandManager.sendMessage("You need to be in a world to reload the finders.");
            return;
        }
        new ReloadFinders().reload();
    }

    private void data(String[] args) {
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z seedcracker data <clear|bits|restore>");
            return;
        }
        if (args[0].equalsIgnoreCase("clear")) {
            Seedcracker.get().reset();
            CommandManager.sendMessage("Cleared data storage");
            return;
        }
        if (args[0].equalsIgnoreCase("bits")) {
            DataStorage storage = Seedcracker.get().getDataStorage();
            CommandManager.sendMessage("You currently have collected " + (int) storage.getBaseBits() + " bits out of " + (int) storage.getWantedBits() + ".");
            CommandManager.sendMessage("You currently have collected " + (int) storage.getLiftingBits() + " bits from liftable structures out of " + 40 + ".");
            return;
        }
        if (args[0].equalsIgnoreCase("restore")) {
            CommandManager.sendMessage("no structures from the previous session found");
            return;
        }
        CommandManager.sendMessage("Usage: .z seedcracker data <clear|bits|restore>");
    }

    private void cracker(String[] args) {
        if (args.length == 0) {
            toggleActive();
            return;
        }
        if (args[0].equalsIgnoreCase("debug")) {
            crackerDebug(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        Boolean flag = parseFlag(args[0]);
        if (flag == null) {
            CommandManager.sendMessage("Usage: .z seedcracker cracker [debug] [ON|OFF]");
            return;
        }
        setActive(flag);
    }

    private void crackerDebug(String[] args) {
        if (args.length == 0) {
            toggleDebug();
            return;
        }
        Boolean flag = parseFlag(args[0]);
        if (flag == null) {
            CommandManager.sendMessage("Usage: .z seedcracker cracker debug [ON|OFF]");
            return;
        }
        setDebug(flag);
    }

    private void setActive(boolean flag) {
        boolean changed = Config.get().active != flag;
        Seedcracker.get().setActive(flag);
        feedback(changed, flag);
    }

    private void toggleActive() {
        boolean next = !Config.get().active;
        Seedcracker.get().setActive(next);
        feedback(true, next);
    }

    private void setDebug(boolean flag) {
        boolean changed = Config.get().debug != flag;
        Seedcracker.get().setDebug(flag);
        feedback(changed, flag);
    }

    private void toggleDebug() {
        boolean next = !Config.get().debug;
        Seedcracker.get().setDebug(next);
        feedback(true, next);
    }

    private void feedback(boolean success, boolean flag) {
        String action = flag ? "enabled" : "disabled";
        if (success) {
            CommandManager.sendMessage("Successfully " + action);
        } else {
            CommandManager.sendMessage("already " + action);
        }
    }

    private void version(String[] args) {
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z seedcracker version <version>");
            return;
        }
        MCVersion version = parseVersion(args[0]);
        if (version == null) {
            CommandManager.sendMessage("Unknown version: " + args[0]);
            return;
        }
        Config.get().setVersion(version);
        CommandManager.sendMessage("Changed version to " + version + ".");
    }

    private void openDatabase() {
        try {
            Util.getPlatform().openUri(DATABASE_URL);
        } catch (Exception e) {
            CommandManager.sendMessage("Failed to open the seed database.");
        }
    }

    private static Config.RenderType parseRenderType(String token) {
        try {
            return Config.RenderType.valueOf(token.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Finder.Type parseFinderType(String token) {
        for (Finder.Type type : Finder.Type.values()) {
            if (type.name().equalsIgnoreCase(token)) return type;
        }
        return null;
    }

    private static Finder.Category parseCategory(String token) {
        for (Finder.Category category : Finder.Category.values()) {
            if (category.name().equalsIgnoreCase(token)) return category;
        }
        return null;
    }

    private static Boolean parseFlag(String token) {
        if (token.equalsIgnoreCase("ON")) return Boolean.TRUE;
        if (token.equalsIgnoreCase("OFF")) return Boolean.FALSE;
        return null;
    }

    private static MCVersion parseVersion(String token) {
        for (MCVersion version : MCVersion.values()) {
            if (version.isOlderThan(MCVersion.v1_8)) continue;
            if (version.name.equalsIgnoreCase(token)) return version;
        }
        return null;
    }

    private static List<String> finderTypeNames() {
        List<String> names = new ArrayList<>();
        for (Finder.Type type : Finder.Type.values()) {
            names.add(type.name());
        }
        return names;
    }

    private static List<String> finderCategoryNames() {
        List<String> names = new ArrayList<>();
        for (Finder.Category category : Finder.Category.values()) {
            names.add(category.name());
        }
        return names;
    }

    private static List<String> versionNames() {
        List<String> names = new ArrayList<>();
        for (MCVersion version : MCVersion.values()) {
            if (version.isOlderThan(MCVersion.v1_8)) continue;
            names.add(version.name);
        }
        return names;
    }

    /** Human-readable label matching SeedcrackerX's {@code finder.*} lang keys. */
    private static String finderName(Finder.Type type) {
        return switch (type) {
            case BURIED_TREASURE -> "Buried treasures";
            case DESERT_TEMPLE -> "Desert temples";
            case END_CITY -> "End cities";
            case JUNGLE_TEMPLE -> "Jungle temples";
            case MONUMENT -> "Ocean monuments";
            case SWAMP_HUT -> "Witch huts";
            case SHIPWRECK -> "Shipwrecks";
            case PILLAGER_OUTPOST -> "Pillager Outposts";
            case IGLOO -> "Igloos";
            case TRIAL_CHAMBERS -> "Trial Chambers";
            case END_PILLARS -> "End pillars";
            case END_GATEWAY -> "End gateways";
            case DUNGEON -> "Dungeons";
            case EMERALD_ORE -> "Emerald ores";
            case DESERT_WELL -> "Desert wells";
            case WARPED_FUNGUS -> "Warped fungus";
            case BIOME -> "Biomes";
        };
    }
}
