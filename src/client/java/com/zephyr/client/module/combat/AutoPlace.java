package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;

/**
 * Automatically places a block on an entity that was just attacked. In WEB mode a cobweb is
 * placed at the victim's feet; in LAVA mode a lava bucket is emptied over it. The actual
 * placement is performed by the AutoPlace attack mixin, which reads the selected mode.
 */
public final class AutoPlace extends Module {
    public static final AutoPlace INSTANCE = new AutoPlace();
    private AutoPlace() {
        super("Auto Place", "Automatically places a specified block on a hit entity", Category.COMBAT);
        addSetting(mode);
    }
    private final EnumSetting<AutoPlace.Mode> mode = new EnumSetting<>("Mode", AutoPlace.Mode.WEB);

    /** The type of block to place on attacked entities. */
    public enum Mode {
        /** Place a cobweb on the hit entity. */
        WEB,
        /** Empty a lava bucket onto the hit entity. */
        LAVA
    }

    /** Returns the currently selected placement mode. */
    public static Mode getMode() {return AutoPlace.INSTANCE.mode.get();}

    /** No per-tick logic; placement is driven by the AutoPlace attack mixin. */
    @Override
    public void tick(Minecraft client) {

    }
}
