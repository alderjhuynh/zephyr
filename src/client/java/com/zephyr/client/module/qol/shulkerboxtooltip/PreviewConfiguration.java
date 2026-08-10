package com.zephyr.client.module.qol.shulkerboxtooltip;

/**
 * Read-only snapshot of the module settings consumed by the preview renderers.
 *
 * @param defaultMaxRowSize The default max number of items in a preview row.
 * @param mergeItems        Whether similar stacks are merged together in compact preview mode.
 * @param shortItemCounts   Whether large item counts use suffixes (e.g. 1,000,000 -> 1M).
 * @param useColors         Whether the preview window uses the item's color.
 */
public record PreviewConfiguration(int defaultMaxRowSize, boolean mergeItems, boolean shortItemCounts,
                                   boolean useColors) {
}
