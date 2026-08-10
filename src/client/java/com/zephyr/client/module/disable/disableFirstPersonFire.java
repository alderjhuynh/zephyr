package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.EnumSetting;

/**
 * Disable-category module that alters the first-person fire overlay rendered while
 * the player is on fire. It is backed by {@code ScreenEffectRendererMixin}, which
 * intercepts {@code ScreenEffectRenderer#submitFire}. The {@link #mode} setting
 * chooses whether the fire overlay is hidden entirely or merely pushed further down
 * the screen.
 */
public final class disableFirstPersonFire extends Module {
    public static final disableFirstPersonFire INSTANCE = new disableFirstPersonFire();

    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.LOWER);

    private disableFirstPersonFire() {
        super("Disable First-Person Fire", "Lowers or removes the first-person fire overlay while on fire", Category.DISABLE);
        addSetting(mode);
    }

    /**
     * The fire overlay treatment: {@code LOWER} shifts it down the screen and
     * {@code DISABLE} removes it entirely.
     */
    public enum Mode {
        LOWER,
        DISABLE
    }

    /**
     * Returns the currently selected fire overlay mode.
     *
     * @return the active {@link Mode}
     */
    public Mode getMode() {
        return mode.get();
    }
}
