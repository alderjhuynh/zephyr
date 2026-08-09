package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;

public final class TimeChanger extends Module {
    public static final TimeChanger INSTANCE = new TimeChanger();

    public final NumberSetting time = new NumberSetting("Time", 6000D, 0D, 24000D, 100D);

    private TimeChanger() {
        super("Time Changer", "Changes the time of day client-side", Category.QOL);
        addSetting(time);
    }
}
