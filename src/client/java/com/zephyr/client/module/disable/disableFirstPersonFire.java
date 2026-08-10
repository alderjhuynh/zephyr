package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.EnumSetting;

public final class disableFirstPersonFire extends Module {
    public static final disableFirstPersonFire INSTANCE = new disableFirstPersonFire();

    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.LOWER);

    private disableFirstPersonFire() {
        super("Disable First-Person Fire", "Lowers or removes the first-person fire overlay while on fire", Category.DISABLE);
        addSetting(mode);
    }

    public enum Mode {
        LOWER,
        DISABLE
    }

    public Mode getMode() {
        return mode.get();
    }
}
