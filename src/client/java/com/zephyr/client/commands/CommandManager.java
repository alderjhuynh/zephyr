package com.zephyr.client.commands;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CommandManager {
    private static final Map<String, Command> COMMANDS = new LinkedHashMap<>();

    private CommandManager() {
    }

    public static void register(Command command) {
        COMMANDS.put(command.getName().toLowerCase(), command);
    }

    public static List<Command> getCommands() {
        return Collections.unmodifiableList(new ArrayList<>(COMMANDS.values()));
    }

    public static Command resolve(String text, String prefix) {
        if (prefix == null || prefix.isEmpty() || !text.startsWith(prefix)) {
            return null;
        }
        String body = text.substring(prefix.length()).trim();
        if (body.isEmpty()) {
            return null;
        }
        return COMMANDS.get(tokenize(body).get(0).toLowerCase(Locale.ROOT));
    }

    public static Suggestions suggest(String text, String prefix, int cursor) {
        if (prefix == null || prefix.isEmpty() || !text.startsWith(prefix)) {
            return null;
        }
        if (cursor <= prefix.length()) {
            return new Suggestions(StringRange.between(cursor, cursor), Collections.emptyList());
        }

        String upToCursor = text.substring(0, cursor);
        int wordStart = cursor;
        while (wordStart > 0 && !Character.isWhitespace(upToCursor.charAt(wordStart - 1))) {
            wordStart--;
        }
        int rangeStart = Math.min(Math.max(wordStart, prefix.length()), cursor);
        String partial = upToCursor.substring(rangeStart);
        String completed = upToCursor.substring(prefix.length(), rangeStart).trim();

        List<String> candidates = new ArrayList<>();
        if (completed.isEmpty()) {
            for (Command command : COMMANDS.values()) {
                candidates.add(command.getName());
            }
        } else {
            List<String> tokens = tokenize(completed);
            if (!tokens.isEmpty()) {
                Command command = COMMANDS.get(tokens.get(0).toLowerCase(Locale.ROOT));
                if (command != null) {
                    String[] args = new String[tokens.size()];
                    for (int i = 1; i < tokens.size(); i++) {
                        args[i - 1] = tokens.get(i);
                    }
                    args[tokens.size() - 1] = partial;
                    candidates.addAll(command.suggest(args));
                }
            }
        }

        StringRange range = StringRange.between(rangeStart, cursor);
        List<Suggestion> suggestions = new ArrayList<>();
        String filter = partial.startsWith("\"") ? partial.substring(1) : partial;
        String filterLower = filter.toLowerCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(filterLower)) {
                String suggestionText = needsQuoting(candidate) ? "\"" + candidate + "\"" : candidate;
                suggestions.add(new Suggestion(range, suggestionText));
            }
        }
        return new Suggestions(range, suggestions);
    }

    private static boolean needsQuoting(String candidate) {
        for (int i = 0; i < candidate.length(); i++) {
            if (Character.isWhitespace(candidate.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** Splits on whitespace, keeping double-quoted spans together and stripping the quotes. */
    private static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    public static boolean dispatch(Minecraft client, String message) {
        String prefix = CommandPrefixHandler.currentPrefix();
        if (prefix == null || prefix.isEmpty() || !message.startsWith(prefix)) {
            return false;
        }

        String body = message.substring(prefix.length()).trim();
        if (!body.isEmpty()) {
            List<String> parts = tokenize(body);
            String name = parts.get(0).toLowerCase(Locale.ROOT);
            List<String> args = parts.subList(1, parts.size());

            Command command = COMMANDS.get(name);
            if (command != null) {
                command.execute(args.toArray(new String[0]));
            } else {
                sendMessage("Unknown command: " + name);
            }
        }
        return true;
    }

    /** Adds a system-style message to the player's chat, without sending anything to the server. */
    public static void sendMessage(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui != null && client.gui.hud != null && client.gui.hud.getChat() != null) {
            client.gui.hud.getChat().addClientSystemMessage(Component.literal(text));
        }
    }
}
