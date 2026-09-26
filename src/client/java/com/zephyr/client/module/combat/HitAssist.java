package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;

/**
 * When an attack misses but the local player was looking within {@link #angle} degrees of a
 * valid entity, sends the attack packet anyway. Consumed by the HitAssist Minecraft mixin,
 * which also factors in the {@code Reach} module.
 */
public final class HitAssist extends Module {
    public static final HitAssist INSTANCE = new HitAssist();

    /** Maximum angle (in degrees) between the player's look direction and an entity for a missed attack to be redirected. */
    public final NumberSetting angle = new NumberSetting("Angle", 30D, 0D, 90D, 1D);

    private HitAssist() {
        super("Hit Assist", "Sends the attack packet anyway when you miss, if you were looking close enough to an entity", Category.COMBAT);
        addSetting(angle);
    }
}
