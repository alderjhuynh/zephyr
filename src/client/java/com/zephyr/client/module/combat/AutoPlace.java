package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.ARBUniformBufferObject;

public final class AutoPlace extends Module {
    public static final AutoPlace INSTANCE = new AutoPlace();
    private AutoPlace() {
        super("Auto Place", "Automatically places a specified block on a hit entity", Category.COMBAT);
        addSetting(mode);
    }
    private final EnumSetting<AutoPlace.Mode> mode = new EnumSetting<>("Mode", AutoPlace.Mode.WEB);
    public enum Mode {
        WEB,
        LAVA
    }

    public static Mode getMode() {return AutoPlace.INSTANCE.mode.get();}

    @Override
    public void tick(Minecraft client) {

    }
}
