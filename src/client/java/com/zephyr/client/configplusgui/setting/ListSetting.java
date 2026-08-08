package com.zephyr.client.configplusgui.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ListSetting extends Setting<List<ListSetting.ListEntry>> {
    private final List<ListEntry> entries = new ArrayList<>();

    public ListSetting(String name) {
        super(name, Collections.emptyList());
    }

    @Override
    public List<ListEntry> get() {
        return Collections.unmodifiableList(entries);
    }

    public void add(String blockName, String color) {
        entries.add(new ListEntry(blockName, color));
    }

    public void setEntry(int index, String blockName, String color) {
        if (index >= 0 && index < entries.size()) {
            entries.set(index, new ListEntry(blockName, color));
        }
    }

    public void remove(int index) {
        if (index >= 0 && index < entries.size()) {
            entries.remove(index);
        }
    }

    public void clear() {
        entries.clear();
    }

    public record ListEntry(String blockName, String color) {
    }

   public static int parseColor(String color, int fallback) {
        if (color == null) return fallback;
        String trimmed = color.trim();
        if (trimmed.isEmpty()) return fallback;

        String hex = trimmed;
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }

        if (hex.length() != 6 && hex.length() != 8) return fallback;

        try {
            long value = Long.parseLong(hex, 16);
            if (hex.length() == 6) {
                return 0xFF000000 | (int) value;
            }
            return (int) value;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
