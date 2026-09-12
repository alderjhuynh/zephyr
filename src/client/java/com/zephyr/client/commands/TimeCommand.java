package com.zephyr.client.commands;

import com.zephyr.client.module.qol.TimeChanger;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;

/**
 * Port of {@code ctime}. Uses TimeChanger module for client-side time.
 */
public final class TimeCommand extends Command {
    public static final TimeCommand INSTANCE = new TimeCommand();

    private TimeCommand() {
        super("time", "Client time: .z time <query|set <time>|reset> (alias: ctime)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("query", "set", "reset");
        if (args.length == 2 && args[0].equalsIgnoreCase("query")) return List.of("day","daytime","gametime");
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) return List.of("day","noon","night","midnight");
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { CommandManager.sendMessage("You must be in a world"); return; }
        if (args.length == 0) { CommandManager.sendMessage("Usage: .z time <query|set|reset> ..."); return; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "query" -> {
                if (args.length < 2) { CommandManager.sendMessage("Usage: .z time query <day|daytime|gametime>"); return; }
                String q = args[1].toLowerCase(Locale.ROOT);
                long time;
                if (q.equals("day")) time = (mc.level.getOverworldClockTime() / SharedConstants.TICKS_PER_GAME_DAY % 2147483647L);
                else if (q.equals("daytime")) time = (mc.level.getOverworldClockTime() % SharedConstants.TICKS_PER_GAME_DAY);
                else if (q.equals("gametime")) time = (mc.level.getGameTime() % 2147483647L);
                else { CommandManager.sendMessage("Unknown query: " + q); return; }
                CommandManager.sendMessage("Time [" + q + "]: " + time);
            }
            case "set" -> {
                if (args.length < 2) { CommandManager.sendMessage("Usage: .z time set <day|noon|night|midnight|<time>>"); return; }
                String t = args[1].toLowerCase(Locale.ROOT);
                long ticks;
                switch (t) {
                    case "day" -> ticks = 1000;
                    case "noon" -> ticks = 6000;
                    case "night" -> ticks = 13000;
                    case "midnight" -> ticks = 18000;
                    default -> {
                        try { ticks = parseTime(t); } catch (Exception e) { CommandManager.sendMessage("Invalid time: " + t); return; }
                    }
                }
                TimeChanger.INSTANCE.time.set((double)ticks);
                TimeChanger.INSTANCE.setEnabled(true);
                CommandManager.sendMessage("Client time set to " + ticks + " ticks");
            }
            case "reset" -> {
                TimeChanger.INSTANCE.setEnabled(false);
                CommandManager.sendMessage("Client time reset to server time");
            }
            default -> CommandManager.sendMessage("Usage: .z time <query|set|reset> ...");
        }
    }

    private long parseTime(String s) {
        // accept integer or time string like 14:30
        if (s.contains(":")) {
            String[] parts = s.split(":");
            int h = Integer.parseInt(parts[0]); int m = Integer.parseInt(parts[1]);
            return (h*1000 - 6000 + 24000) % 24000 + m*1000/60;
        }
        return Long.parseLong(s);
    }
}
