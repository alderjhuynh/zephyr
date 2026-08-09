package com.zephyr.client.module.qol.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Color palette + component helpers for the Jade overlay, loosely matching Jade's
 * default "Dark" theme but tinted to blend with Zephyr's own HUD panels.
 */
public final class JadeColors {
    public static final int TITLE = 0xFFF2EAFB;
    public static final int NORMAL = 0xFFE6DDF2;
    public static final int INFO = 0xFFAFA5C0;
    public static final int SUCCESS = 0xFF6ADE8A;
    public static final int DANGER = 0xFFF87070;
    public static final int MOD_NAME = 0xFF8A8794;
    public static final int HEALTH = 0xFFE54B4B;
    public static final int ARMOR = 0xFF9AA5B1;
    public static final int BAR_BACKGROUND = 0xFF2B2733;

    private JadeColors() {
    }

    public static MutableComponent colored(String text, int color) {
        return Component.literal(text).setStyle(Style.EMPTY.withColor(color));
    }

    public static MutableComponent colored(Component text, int color) {
        return Component.literal(text.getString()).setStyle(Style.EMPTY.withColor(color));
    }
}
