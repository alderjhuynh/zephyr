package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;

public final class HitAssist extends Module {
    public static final HitAssist INSTANCE = new HitAssist();

    public final NumberSetting angle = new NumberSetting("Angle", 30D, 0D, 90D, 1D);

    private HitAssist() {
        super("Hit Assist", "Sends the attack packet anyway when you miss, if you were looking close enough to an entity", Category.COMBAT);
        addSetting(angle);
    }
}
