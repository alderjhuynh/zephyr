package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;

/**
 * Overrides the client-side time of day, letting the player set any time they
 * like without affecting the server. The configured time is applied by the
 * associated world-time mixin while this module is enabled.
 */
public final class TimeChanger extends Module {
    public static final TimeChanger INSTANCE = new TimeChanger();

    /** The time of day (in ticks, 0-24000) shown while the module is enabled. */
    public final NumberSetting time = new NumberSetting("Time", 6000D, 0D, 24000D, 100D);

    private TimeChanger() {
        super("Time Changer", "Changes the time of day client-side", Category.QOL);
        addSetting(time);
    }
}
