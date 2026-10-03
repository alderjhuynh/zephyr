package com.zephyr.client.module.disable;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;

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

    // 26.3 uses client.levelExtractor.allChanged(); 1.21.1 equivalent is
    // client.levelRenderer.allChanged().
    private static void requestChunkRebuild() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.levelRenderer != null) {
            client.levelRenderer.allChanged();
        }
    }
}
