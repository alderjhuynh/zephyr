package com.zephyr.client.module.qol.jade.provider.vanilla;

import com.zephyr.client.module.qol.jade.Jade;
import com.zephyr.client.module.qol.jade.JadeColors;
import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.access.EntityAccessor;
import com.zephyr.client.module.qol.jade.provider.IComponentProvider;
import com.zephyr.client.module.qol.jade.render.TextElement;
import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

/**
 * Shows the item held by an item frame, mirroring Jade's {@code ItemFrameProvider}.
 */
public class ItemFrameProvider implements IComponentProvider {
    public static final ItemFrameProvider INSTANCE = new ItemFrameProvider();

    @Override
    public void appendTooltip(Tooltip tooltip, Accessor accessor) {
        if (!Jade.INSTANCE.showItemFrame() || !(accessor instanceof EntityAccessor entity)) {
            return;
        }
        if (!(entity.getEntity() instanceof ItemFrame itemFrame)) {
            return;
        }
        ItemStack stack = itemFrame.getItem();
        if (!stack.isEmpty()) {
            tooltip.add(new TextElement(stack.getHoverName(), JadeColors.INFO, true));
        }
    }
}
