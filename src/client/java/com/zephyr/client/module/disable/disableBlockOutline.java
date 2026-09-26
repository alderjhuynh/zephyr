package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that hides the black outline rendered around the block
 * currently being targeted. A simple toggle with no settings; it is backed by
 * {@code LevelRendererMixin}, which cancels {@code LevelRenderer#submitBlockOutline}
 * while enabled.
 */
public final class disableBlockOutline extends Module {
    public static final disableBlockOutline INSTANCE = new disableBlockOutline();
    private disableBlockOutline() { super("Disable Block Outline", "Hides the black outline on the targeted block", Category.DISABLE); }
}
