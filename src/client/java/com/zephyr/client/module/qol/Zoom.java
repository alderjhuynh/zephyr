package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.util.Mth;

/**
 * Smoothly brings the player's FOV down to a configurable zoom level while
 * enabled, then smoothly returns it to the original FOV when disabled. The
 * smoothing is driven per-frame from the {@code Camera.calculateFov} mixin so
 * the transition is fluid rather than a snap.
 */
public final class Zoom extends Module {
    public static final Zoom INSTANCE = new Zoom();

    /** Target FOV while zoomed in. */
    private final NumberSetting zoomFov = new NumberSetting("Zoom FOV", 30.0, 1.0, 120.0, 1.0);
    /** Lerp factor applied per frame; higher values snap faster, lower values are silkier. */
    private final NumberSetting smoothness = new NumberSetting("Smoothness", 0.1, 0.01, 1.0, 0.01);

    private float smoothedFov;
    private boolean initialized;
    /** True for the frames after {@link #onDisable()} until the FOV has returned to normal. */
    private boolean zoomingOut;

    private Zoom() {
        super("Zoom", "Smoothly zooms your FOV in to a custom level while enabled", Category.QOL);
        addSetting(zoomFov);
        addSetting(smoothness);
    }

    /** Clears the zoom-out flag so the FOV transitions toward the zoom target. */
    @Override
    protected void onEnable() {
        zoomingOut = false;
    }

    /** Marks the module as returning to the original FOV so it settles smoothly. */
    @Override
    protected void onDisable() {
        zoomingOut = true;
    }

    /**
     * True only while the module is actually changing the FOV: either zooming in
     * or still returning to the original FOV after a disable. When idle the mixin
     * skips entirely so vanilla FOV calculation (including its own world-join
     * settling) is never touched.
     */
    public boolean isActive() {
        return isEnabled() || zoomingOut;
    }

    /**
     * Called once per rendered frame from the {@code Camera.calculateFov} mixin.
     * While enabled, drifts the smoothed FOV toward the zoom target; after a
     * disable, drifts it back toward the original FOV until it settles. Disabled
     * and settled modules return the vanilla value untouched.
     */
    public float apply(float vanillaFov) {
        if (!initialized) {
            smoothedFov = vanillaFov;
            initialized = true;
            zoomingOut = false;
        }
        if (isEnabled()) {
            zoomingOut = false;
            smoothedFov = Mth.lerp(smoothness.get().floatValue(), smoothedFov, (float) (double) zoomFov.get());
            return smoothedFov;
        }
        if (zoomingOut) {
            smoothedFov = Mth.lerp(smoothness.get().floatValue(), smoothedFov, vanillaFov);
            if (Math.abs(smoothedFov - vanillaFov) < 0.01F) {
                zoomingOut = false;
                return vanillaFov;
            }
            return smoothedFov;
        }
        return vanillaFov;
    }
}
