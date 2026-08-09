package com.zephyr.client.module.qol.shulkerboxtooltip;

/**
 * The type of preview to draw.
 */
public enum PreviewType {
    /**
     * Preview is not present.
     */
    NO_PREVIEW,

    /**
     * Compact mode: similar items are grouped together and empty slots are not shown.
     */
    COMPACT,

    /**
     * Full mode: all stacks are shown in their respective slots, empty slots are also displayed.
     */
    FULL
}
