package com.zephyr.client.module.qol.jade.provider.vanilla;

import com.zephyr.client.module.qol.jade.JadeColors;
import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.access.BlockAccessor;
import com.zephyr.client.module.qol.jade.access.EntityAccessor;
import com.zephyr.client.module.qol.jade.provider.IComponentProvider;
import com.zephyr.client.module.qol.jade.render.TextElement;
import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Always-first row: the display name of the targeted block or entity, mirroring
 * Jade's core title provider.
 */
public class NameProvider implements IComponentProvider {
    public static final NameProvider INSTANCE = new NameProvider();

    /**
     * Appends the display name of the targeted block or entity as the first
     * (title) row.
     */
    @Override
    public void appendTooltip(Tooltip tooltip, Accessor accessor) {
        Component name = accessor instanceof BlockAccessor block
                ? block.getBlock().getName()
                : ((EntityAccessor) accessor).getEntity().getName();
        tooltip.add(new TextElement(name, JadeColors.TITLE, true));
    }
}
