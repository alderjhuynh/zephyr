package com.zephyr.client.module.qol.jade.provider.vanilla;

import com.zephyr.client.module.qol.jade.Jade;
import com.zephyr.client.module.qol.jade.JadeColors;
import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.access.BlockAccessor;
import com.zephyr.client.module.qol.jade.provider.IComponentProvider;
import com.zephyr.client.module.qol.jade.render.TextElement;
import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;

/**
 * Lists the block state properties that differ from the block's default state
 * (e.g. "facing: north", "half: upper", "lit: true"), mirroring Jade's block-state
 * details. Default-valued properties are skipped to keep the panel short.
 */
public class BlockStatesProvider implements IComponentProvider {
    public static final BlockStatesProvider INSTANCE = new BlockStatesProvider();

    @Override
    public void appendTooltip(Tooltip tooltip, Accessor accessor) {
        if (!Jade.INSTANCE.showBlockStates() || !(accessor instanceof BlockAccessor block)) {
            return;
        }
        BlockState state = block.getBlockState();
        state.getValues().forEach((property, value) -> addStateLine(tooltip, state, property, value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Comparable<T>> void addStateLine(Tooltip tooltip, BlockState state, Property<?> propertyRaw, Comparable<?> valueRaw) {
        Property<T> property = (Property<T>) propertyRaw;
        T current = state.getValue(property);
        BlockState defaults = state.getBlock().defaultBlockState();
        if (defaults.hasProperty(property) && defaults.getValue(property).equals(current)) {
            return;
        }
        tooltip.add(new TextElement(
                Component.literal(property.getName() + ": " + valueRaw),
                JadeColors.INFO,
                true));
    }
}
