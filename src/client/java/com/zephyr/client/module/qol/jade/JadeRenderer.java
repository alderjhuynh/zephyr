package com.zephyr.client.module.qol.jade;

import com.mojang.blaze3d.platform.Window;
import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.provider.ProviderRegistry;
import com.zephyr.client.module.qol.jade.ray.RayTracer;
import com.zephyr.client.module.qol.jade.render.BoxElement;
import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Drives the overlay: every tick it ray-casts for the target, gathers provider
 * content into a {@link Tooltip}, and caches the measured {@link BoxElement}. Each
 * frame it positions the box on screen and eases its alpha in/out, so the tooltip
 * fades away instead of popping when the crosshair leaves the target — the same
 * "linger" behaviour as Jade's {@code OverlayRenderer}.
 */
public final class JadeRenderer {
    private static final float FADE_SPEED = 0.6F;
    private static final float INSTANT_SPEED = 8F;

    private static @Nullable BoxElement root;
    private static @Nullable BoxElement linger;
    private static boolean shown;
    private static float alpha;

    /** Static utility; not instantiable. */
    private JadeRenderer() {
    }

    /**
     * Recomputes the overlay content for the current frame: ray-casts for the
     * target, gathers provider lines into a {@link Tooltip}, and (re)measures the
     * cached {@link BoxElement}. Clears the "shown" flag when nothing is targeted.
     *
     * @param client the running Minecraft client
     */
    static void tick(Minecraft client) {
        Accessor accessor = RayTracer.raycast(client, Jade.INSTANCE.extendedReach());
        if (accessor == null) {
            shown = false;
            return;
        }

        Tooltip tooltip = new Tooltip();
        ProviderRegistry.gather(tooltip, accessor);
        if (tooltip.isEmpty()) {
            shown = false;
            return;
        }
        tooltip.setIcon(ProviderRegistry.gatherIcon(accessor));

        BoxElement box = new BoxElement(tooltip);
        box.setTheme(Jade.INSTANCE.borderColor(), Jade.INSTANCE.backgroundColor());
        box.layout();
        root = box;
        shown = true;
    }

    /**
     * Renders the cached overlay at the configured position, easing its alpha
     * toward the target so the tooltip lingers and fades when the crosshair
     * leaves the target.
     *
     * @param graphics the graphics context to draw into
     * @param delta    the partial tick time for smooth alpha interpolation
     */
    public static void render(GuiGraphicsExtractor graphics, float delta) {
        if (root == null && linger == null) {
            return;
        }
        if (shown) {
            linger = root;
        }

        float speed = Jade.INSTANCE.fadeAnimation() ? FADE_SPEED : INSTANT_SPEED;
        alpha = Mth.clamp(alpha + (shown ? speed : -speed) * delta, 0, 1);

        if (alpha <= 0.001F) {
            linger = null;
            root = null;
            return;
        }
        if (linger == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();
        int boxWidth = linger.getWidth();
        int boxHeight = linger.getHeight();

        int x;
        int y;
        switch (Jade.INSTANCE.overlayPosition()) {
            case TOP_LEFT -> {
                x = 0;
                y = 0;
            }
            case TOP_CENTER -> {
                x = (screenWidth - boxWidth) / 2;
                y = 0;
            }
            case TOP_RIGHT -> {
                x = screenWidth - boxWidth;
                y = 0;
            }
            case CENTER -> {
                x = (screenWidth - boxWidth) / 2;
                y = (screenHeight - boxHeight) / 2;
            }
            case BOTTOM_LEFT -> {
                x = 0;
                y = screenHeight - boxHeight;
            }
            case BOTTOM_CENTER -> {
                x = (screenWidth - boxWidth) / 2;
                y = screenHeight - boxHeight;
            }
            default -> {
                x = screenWidth - boxWidth;
                y = screenHeight - boxHeight;
            }
        }
        x += Jade.INSTANCE.xOffset();
        y += Jade.INSTANCE.yOffset();
        x = Mth.clamp(x, 0, Math.max(0, screenWidth - boxWidth));
        y = Mth.clamp(y, 0, Math.max(0, screenHeight - boxHeight));

        linger.render(graphics, x, y, alpha * Jade.INSTANCE.overlayAlpha());
    }

    /** Clears all cached state (used when the module is toggled off). */
    static void reset() {
        root = null;
        linger = null;
        shown = false;
        alpha = 0;
    }
}
