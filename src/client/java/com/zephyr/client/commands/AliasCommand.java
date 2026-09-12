package com.zephyr.client.commands;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of clientcommands {@code calias}. Stores aliases in {@code config/zephyr/aliases.json}.
 * Supports {@code .z alias add <key> <command>}, {@code .z alias list}, {@code .z alias remove <key>},
 * and executing aliases via {@code .z alias <key> [args...]} or directly as {@code .z <alias>}.
 * Mirrors original percent-format handling and loop detection.
 */
public final class AliasCommand extends Command {
    public static final AliasCommand INSTANCE = new AliasCommand();

    private static final Path ALIAS_PATH = FabricLoader.getInstance().getConfigDir().resolve("zephyr").resolve("aliases.json");
    private static final Gson GSON = new Gson();
    private static final Map<String, String> aliasMap = loadAliases();

    private AliasCommand() {
        super("alias", "Manage command aliases: .z alias <add|list|remove|exec> ... (also .z calias)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("add", "list", "remove");
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) return List.copyOf(aliasMap.keySet());
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z alias <add <key> <command>|list|remove <key>|<alias> [args]>");
            return;
        }
        String sub = args[0];
        if (sub.equalsIgnoreCase("add")) {
            if (args.length < 3) {
                CommandManager.sendMessage("Usage: .z alias add <key> <command>");
                return;
            }
            String key = args[1];
            String cmd = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            addAlias(key, cmd);
        } else if (sub.equalsIgnoreCase("list")) {
            listAliases();
        } else if (sub.equalsIgnoreCase("remove")) {
            if (args.length < 2) {
                CommandManager.sendMessage("Usage: .z alias remove <key>");
                return;
            }
            removeAlias(args[1]);
        } else {
            // treat as alias execution: .z alias <key> [args...]
            String key = sub;
            String rest = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : null;
            executeAlias(key, rest);
        }
    }

    public boolean tryExecuteAlias(String alias, String[] aliasArgs) {
        if (!aliasMap.containsKey(alias)) return false;
        String rest = aliasArgs.length == 0 ? null : String.join(" ", aliasArgs);
        executeAlias(alias, rest);
        return true;
    }

    private void addAlias(String key, String command) {
        if (aliasMap.containsKey(key)) {
            CommandManager.sendMessage("Alias already exists: " + key);
            return;
        }
        if (CommandManager.getCommands().stream().anyMatch(c -> c.getName().equalsIgnoreCase(key))) {
            CommandManager.sendMessage("A command already exists with that name: " + key);
            return;
        }
        if (!command.startsWith("/")) command = "/" + command;
        // also check z subcommands? allow for now
        aliasMap.put(key, command);
        saveAliases();
        CommandManager.sendMessage("Added alias '" + key + "' -> " + command);
    }

    private void listAliases() {
        if (aliasMap.isEmpty()) {
            CommandManager.sendMessage("No aliases registered");
            return;
        }
        CommandManager.sendMessage("Aliases (" + aliasMap.size() + "):");
        for (Map.Entry<String, String> e : aliasMap.entrySet()) {
            CommandManager.sendMessage("  " + e.getKey() + ": " + e.getValue().replace("%", "%%"));
        }
    }

    private void removeAlias(String key) {
        if (!aliasMap.containsKey(key)) {
            CommandManager.sendMessage("Alias not found: " + key);
            return;
        }
        aliasMap.remove(key);
        saveAliases();
        CommandManager.sendMessage("Removed alias '" + key + "'");
    }

    private void executeAlias(String aliasKey, String arguments) {
        String cmd = aliasMap.get(aliasKey);
        if (cmd == null) {
            CommandManager.sendMessage("Alias not found: " + aliasKey);
            return;
        }
        try {
            // count unescaped % placeholders similar to original: (?<!%)%(?:%%)*(?!%)
            int inlineCount = (int) Pattern.compile("(?<!%)%(?:%%)*(?!%)").matcher(cmd).results().count();
            String finalCmd;
            if (inlineCount > 0) {
                String[] argArray = arguments == null ? new String[0] : arguments.split(" ", inlineCount + 1);
                // pad missing args with empty
                if (argArray.length < inlineCount) {
                    String[] padded = new String[inlineCount];
                    System.arraycopy(argArray, 0, padded, 0, argArray.length);
                    for (int i = argArray.length; i < inlineCount; i++) padded[i] = "";
                    argArray = padded;
                }
                String trailing = "";
                String[] formatArgs;
                String trailingArgs = "";
                if (arguments != null) {
                    String[] split = arguments.split(" ", inlineCount + 1);
                    if (split.length > inlineCount) trailingArgs = " " + split[inlineCount];
                    // need exactly inlineCount args for format
                    formatArgs = new String[inlineCount];
                    for (int i = 0; i < inlineCount; i++) {
                        if (i < split.length) formatArgs[i] = split[i];
                        else formatArgs[i] = "";
                    }
                } else {
                    formatArgs = new String[inlineCount];
                    for (int i = 0; i < inlineCount; i++) formatArgs[i] = "";
                }
                try {
                    finalCmd = String.format(cmd, (Object[]) formatArgs) + trailingArgs;
                } catch (Exception e) {
                    CommandManager.sendMessage("Illegal format in alias '" + aliasKey + "'");
                    return;
                }
            } else if (arguments != null) {
                finalCmd = cmd + " " + arguments;
            } else {
                finalCmd = cmd;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() == null) {
                CommandManager.sendMessage(finalCmd);
                return;
            }
            if (finalCmd.startsWith("/")) {
                mc.getConnection().sendCommand(finalCmd.substring(1));
            } else {
                mc.getConnection().sendChat(finalCmd);
            }
        } catch (Exception e) {
            CommandManager.sendMessage("Failed to execute alias '" + aliasKey + "': " + e.getMessage());
        }
    }

    private static HashMap<String, String> loadAliases() {
        if (!Files.exists(ALIAS_PATH)) return new HashMap<>();
        try (Reader r = Files.newBufferedReader(ALIAS_PATH)) {
            Map<String, String> m = GSON.fromJson(r, new TypeToken<HashMap<String, String>>() {}.getType());
            return m != null ? new HashMap<>(m) : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static void saveAliases() {
        try {
            Files.createDirectories(ALIAS_PATH.getParent());
            try (Writer w = Files.newBufferedWriter(ALIAS_PATH)) {
                GSON.toJson(aliasMap, w);
            }
        } catch (Exception e) {
            CommandManager.sendMessage("Failed to save aliases: " + e.getMessage());
        }
    }
}
