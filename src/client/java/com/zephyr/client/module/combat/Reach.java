package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/**
 * Extends both block and entity interaction ranges. Values are added to the player's range
 * via the Reach player mixin (which modifies {@code blockInteractionRange} and
 * {@code entityInteractionRange}) and are also read by other modules such as
 * {@code AnchorAura}, {@code KillAura}, {@code HitAssist} and {@code TriggerBot}.
 */
public final class Reach extends Module {
    public static final Reach INSTANCE = new Reach();

    /** Extra block interaction range, in blocks. */
    public final NumberSetting blockReach = new NumberSetting("Block Reach", 1D, 0D, 10.0D, 1D);
    /** Extra entity interaction range, in blocks. */
    public final NumberSetting entityReach = new NumberSetting("Entity Reach", 1D, 0D, 4.0D, 1D);

    private Reach() {
        super("Reach", "Increases reach distance", Category.COMBAT);
        addSetting(blockReach);
        addSetting(entityReach);
    }
}
