package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;

/**
 * Automatically holds down the attack key, mimicking the vanilla "always
 * attack" behavior (similar to holding left-click) without requiring input.
 * Each tick the attack key binding is forced into the pressed state while the
 * module is enabled.
 */
public final class HoldAttack extends Module {
    public static final HoldAttack INSTANCE = new HoldAttack();
    private HoldAttack() {
        super("Hold Attack", "Continually simulates pressing the attack key", Category.QOL);
    }

    /** Forces the attack key to its pressed state for this tick. */
    @Override
    public void tick(Minecraft client) {
        client.options.keyAttack.setDown(true);
    }
}
