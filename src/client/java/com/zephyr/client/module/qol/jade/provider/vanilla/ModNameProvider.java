package com.zephyr.client.module.qol.jade.provider.vanilla;

import com.zephyr.client.module.qol.jade.Jade;
import com.zephyr.client.module.qol.jade.JadeColors;
import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.access.BlockAccessor;
import com.zephyr.client.module.qol.jade.access.EntityAccessor;
import com.zephyr.client.module.qol.jade.provider.IComponentProvider;
import com.zephyr.client.module.qol.jade.provider.ModIdentification;
import com.zephyr.client.module.qol.jade.render.TextElement;
import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Second row: which mod the target belongs to, mirroring Jade's mod-name provider.
 * Toggled by the module's "Show Mod Name" setting.
 */
public class ModNameProvider implements IComponentProvider {
    public static final ModNameProvider INSTANCE = new ModNameProvider();

    /**
     * Appends the mod display name of the target's block or entity type when the
     * "Show Mod Name" setting is enabled.
     */
    @Override
    public void appendTooltip(Tooltip tooltip, Accessor accessor) {
        if (!Jade.INSTANCE.showModName()) {
            return;
        }
        String modName = accessor instanceof BlockAccessor block
                ? ModIdentification.getModName(block.getBlock())
                : ModIdentification.getModName(((EntityAccessor) accessor).getEntity());
        if (modName == null || modName.isEmpty()) {
            return;
        }
        tooltip.add(new TextElement(Component.literal(modName), JadeColors.MOD_NAME, true));
    }
}
