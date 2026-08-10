package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;

/**
 * Automatically holds down the use key, mimicking the vanilla "always use"
 * behavior (similar to holding right-click) without requiring input. Each tick
 * the use key binding is forced into the pressed state while the module is
 * enabled.
 */
public final class HoldUse extends Module {
    public static final HoldUse INSTANCE = new HoldUse();
    private HoldUse() {
        super("Hold Use", "Continually simulates pressing the use key", Category.QOL);
    }

    /** Forces the use key to its pressed state for this tick. */
    @Override
    public void tick(Minecraft client) {
        client.options.keyUse.setDown(true);
    }
}
