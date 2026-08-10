package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;

/**
 * Disable-category module that hides rain weather rendering and mutes rain sounds.
 * A simple toggle with no settings; it is backed by {@code NoRainRenderMixin}, which
 * cancels {@code WeatherEffectRenderer#render}, and {@code NoRainSoundMixin}, which
 * stops sounds whose path contains {@code weather.rain}, while enabled.
 */
public final class disableRainEffects extends Module {
    public static final disableRainEffects INSTANCE = new disableRainEffects();
    private disableRainEffects() { super("Disable Rain", "Hides rain and rain sounds", Category.DISABLE); }
}
