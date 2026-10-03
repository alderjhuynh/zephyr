package com.zephyr.client.module.qol.jade.provider.vanilla;

import com.zephyr.client.module.qol.jade.Jade;
import com.zephyr.client.module.qol.jade.JadeColors;
import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.access.BlockAccessor;
import com.zephyr.client.module.qol.jade.provider.IComponentProvider;
import com.zephyr.client.module.qol.jade.render.TextElement;
import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Shows the honey level of bee nests/hives as a filled bar, mirroring Jade's
 * {@code BeehiveProvider}. Works from the block state (level 0-5) so no server
 * data is needed.
 */
public class BeehiveProvider implements IComponentProvider {
    public static final BeehiveProvider INSTANCE = new BeehiveProvider();

    /**
     * Appends a "Honey: n/5" row for targeted bee nests/hives when the
     * corresponding module setting is enabled.
     */
    @Override
    public void appendTooltip(Tooltip tooltip, Accessor accessor) {
        if (!Jade.INSTANCE.showBeehive() || !(accessor instanceof BlockAccessor block)) {
            return;
        }
        if (block.getBlock() instanceof BeehiveBlock && block.getBlockState().hasProperty(BlockStateProperties.LEVEL_HONEY)) {
            int level = block.getBlockState().getValue(BlockStateProperties.LEVEL_HONEY);
            Component text = Component.literal(level + "/5");
            tooltip.add(new TextElement(Component.literal("Honey: ").append(text), JadeColors.NORMAL, true));
        }
    }
}
