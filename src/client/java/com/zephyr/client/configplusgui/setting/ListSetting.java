package com.zephyr.client.configplusgui.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link Setting} holding an ordered list of {@link ListEntry} values, each pairing a
 * block name with a color string. It backs the block/color pickers (e.g. X-ray block
 * lists) shown as multi-row settings in the click-gui. The list is serialized as an array
 * of {@code {block, color}} objects by {@link com.zephyr.client.configplusgui.config.ConfigManager}.
 */
public final class ListSetting extends Setting<List<ListSetting.ListEntry>> {
    private final List<ListEntry> entries = new ArrayList<>();

    /** Creates an empty list setting. */
    public ListSetting(String name) {
        super(name, Collections.emptyList());
    }

    @Override
    public List<ListEntry> get() {
        return Collections.unmodifiableList(entries);
    }

    /** Appends a new block/color entry. */
    public void add(String blockName, String color) {
        entries.add(new ListEntry(blockName, color));
    }

    /** Replaces the entry at {@code index}; out-of-range indices are ignored. */
    public void setEntry(int index, String blockName, String color) {
        if (index >= 0 && index < entries.size()) {
            entries.set(index, new ListEntry(blockName, color));
        }
    }

    /** Removes the entry at {@code index}; out-of-range indices are ignored. */
    public void remove(int index) {
        if (index >= 0 && index < entries.size()) {
            entries.remove(index);
        }
    }

    /** Removes all entries. */
    public void clear() {
        entries.clear();
    }

    /** A single list row: a block identifier and an associated color string. */
    public record ListEntry(String blockName, String color) {
    }

   /**
    * Parses a color string as hex ({@code #RRGGBB}, {@code 0xRRGGBB[AA]} or bare) into an
    * ARGB int; 6-digit values are treated as fully opaque. Returns {@code fallback} for
    * null/empty/malformed input.
    */
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
