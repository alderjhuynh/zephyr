package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;

/**
 * Disable-category module that hides fire rendering: world fire blocks (fire
 * and soul fire) and the flame billboard on burning entities. A simple toggle
 * with no settings; it is backed by {@code ModelBlockRendererMixin}, which
 * skips tesselation of any {@code BaseFireBlock}, and by
 * {@code EntityRenderDispatcherMixin}, which skips {@code submitFlame} for
 * entities with the fire animation. The first-person fire overlay is handled
 * separately by {@code disableFirstPersonFire}.
 */
public final class disableFireRendering extends Module {
    public static final disableFireRendering INSTANCE = new disableFireRendering();

    private disableFireRendering() {
        super("Disable Fire Rendering", "Hides world fire and burning-entity flames", Category.DISABLE);
    }

    @Override
    protected void onEnable() {
        requestChunkRebuild();
    }

    @Override
    protected void onDisable() {
        requestChunkRebuild();
    }

    /**
     * Forces fire sections to re-bake so the change takes effect immediately.
     * Fire quads are baked into chunk meshes, so without a rebuild
     * already-visible fire would keep its old visibility.
     */
    private static void requestChunkRebuild() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.levelExtractor != null) {
            client.levelExtractor.allChanged();
        }
    }
}
