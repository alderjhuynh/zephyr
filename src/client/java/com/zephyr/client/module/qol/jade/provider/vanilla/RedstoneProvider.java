package com.zephyr.client.module.qol.jade.provider.vanilla;

import com.zephyr.client.module.qol.jade.Jade;
import com.zephyr.client.module.qol.jade.JadeColors;
import com.zephyr.client.module.qol.jade.access.Accessor;
import com.zephyr.client.module.qol.jade.access.BlockAccessor;
import com.zephyr.client.module.qol.jade.provider.IComponentProvider;
import com.zephyr.client.module.qol.jade.render.TextElement;
import com.zephyr.client.module.qol.jade.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;

/**
 * Redstone info, mirroring Jade's {@code RedstoneProvider}: lever on/off, repeater
 * delay, comparator mode, and the strong-signal level of blocks with a POWER state
 * (redstone wire, target block, ...). Comparator/sculk-sensor output needs server
 * data, so those are omitted from this client-only port.
 */
public class RedstoneProvider implements IComponentProvider {
    public static final RedstoneProvider INSTANCE = new RedstoneProvider();

    /**
     * Appends redstone detail rows (lever state, repeater delay, comparator mode,
     * strong signal power) for targeted blocks when the corresponding module
     * setting is enabled.
     */
    @Override
    public void appendTooltip(Tooltip tooltip, Accessor accessor) {
        if (!Jade.INSTANCE.showRedstone() || !(accessor instanceof BlockAccessor block)) {
            return;
        }
        BlockState state = block.getBlockState();
        Block blockType = state.getBlock();

        if (blockType instanceof LeverBlock) {
            boolean powered = state.getValue(BlockStateProperties.POWERED);
            Component stateComponent = powered
                    ? JadeColors.colored("On", JadeColors.SUCCESS)
                    : JadeColors.colored("Off", JadeColors.DANGER);
            tooltip.add(new TextElement(Component.literal("State: ").append(stateComponent), JadeColors.NORMAL, true));
            return;
        }

        if (blockType == Blocks.REPEATER) {
            int delay = state.getValue(BlockStateProperties.DELAY);
            tooltip.add(new TextElement(Component.literal("Delay: " + delay + " tick" + (delay == 1 ? "" : "s")),
                    JadeColors.NORMAL, true));
            return;
        }

        if (blockType == Blocks.COMPARATOR) {
            ComparatorMode mode = state.getValue(BlockStateProperties.MODE_COMPARATOR);
            String modeName = mode == ComparatorMode.COMPARE ? "Compare" : "Subtract";
            tooltip.add(new TextElement(Component.literal("Mode: " + modeName), JadeColors.NORMAL, true));
            return;
        }

        if (state.hasProperty(BlockStateProperties.POWER)) {
            int power = state.getValue(BlockStateProperties.POWER);
            if (power > 0) {
                tooltip.add(new TextElement(Component.literal("Power: " + power), JadeColors.NORMAL, true));
            }
        }
    }
}
