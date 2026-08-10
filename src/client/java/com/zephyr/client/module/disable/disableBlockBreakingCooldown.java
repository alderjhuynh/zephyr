package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that removes the brief block-breaking delay between
 * consecutive block destructions. A simple toggle with no settings; it is backed by
 * {@code BlockBreakingCooldownMixin}, which zeroes the game mode's {@code destroyDelay}
 * while enabled.
 */
public final class disableBlockBreakingCooldown extends Module {
    public static final disableBlockBreakingCooldown INSTANCE = new disableBlockBreakingCooldown();
    private disableBlockBreakingCooldown() { super("Disable Block Cooldown", "Removes block breaking cooldown", Category.DISABLE); }
}
