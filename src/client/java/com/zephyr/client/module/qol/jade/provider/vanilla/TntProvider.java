package com.zephyr.client.module.qol.jade.provider.vanilla;

import com.zephyr.client.module.qol.jade.Jade;
import com.zephyr.client.module.qol.jade.JadeColors;
import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.access.BlockAccessor;
import com.zephyr.client.module.qol.jade.provider.IComponentProvider;
import com.zephyr.client.module.qol.jade.render.TextElement;
import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Warns about "unstable" TNT that ignites the moment it is touched, mirroring
 * Jade's {@code TNTStabilityProvider}. Only shows the row for primed-by-touch
 * blocks, so ordinary TNT stays clean.
 */
public class TntProvider implements IComponentProvider {
    public static final TntProvider INSTANCE = new TntProvider();

    @Override
    public void appendTooltip(Tooltip tooltip, Accessor accessor) {
        if (!Jade.INSTANCE.showTntStability() || !(accessor instanceof BlockAccessor block)) {
            return;
        }
        if (block.getBlock() instanceof TntBlock && block.getBlockState().getValue(BlockStateProperties.UNSTABLE)) {
            tooltip.add(new TextElement(JadeColors.colored("Unstable", JadeColors.DANGER), JadeColors.NORMAL, true));
        }
    }
}
