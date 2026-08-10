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
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Locale;

/**
 * Shows crop growth as a percentage, mirroring Jade's {@code CropProgressProvider}.
 * Ported straight from the original: crops use their own age/max-age, anything
 * else with an age property (nether wart, bonemealable plants) uses the vanilla
 * property thresholds.
 */
public class CropProgressProvider implements IComponentProvider {
    public static final CropProgressProvider INSTANCE = new CropProgressProvider();

    /**
     * Appends a "Growth: X%" (or "Mature") row for targeted growing blocks when
     * the corresponding module setting is enabled.
     */
    @Override
    public void appendTooltip(Tooltip tooltip, Accessor accessor) {
        if (!Jade.INSTANCE.showCropProgress() || !(accessor instanceof BlockAccessor block)) {
            return;
        }
        BlockState state = block.getBlockState();
        Block blockType = state.getBlock();

        if (blockType instanceof CropBlock crop) {
            addMaturity(tooltip, crop.getAge(state) / (float) crop.getMaxAge());
        } else if (blockType instanceof NetherWartBlock || blockType instanceof BonemealableBlock) {
            if (state.hasProperty(BlockStateProperties.AGE_2)) {
                addMaturity(tooltip, state.getValue(BlockStateProperties.AGE_2) / 2F);
            } else if (state.hasProperty(BlockStateProperties.AGE_3)) {
                addMaturity(tooltip, state.getValue(BlockStateProperties.AGE_3) / 3F);
            } else if (state.hasProperty(BlockStateProperties.AGE_4)) {
                addMaturity(tooltip, state.getValue(BlockStateProperties.AGE_4) / 4F);
            } else if (state.hasProperty(BlockStateProperties.AGE_5)) {
                addMaturity(tooltip, state.getValue(BlockStateProperties.AGE_5) / 5F);
            } else if (state.hasProperty(BlockStateProperties.AGE_7)) {
                addMaturity(tooltip, state.getValue(BlockStateProperties.AGE_7) / 7F);
            } else if (state.hasProperty(BlockStateProperties.AGE_15)) {
                addMaturity(tooltip, state.getValue(BlockStateProperties.AGE_15) / 15F);
            }
        }
    }

    private static void addMaturity(Tooltip tooltip, float growthValue) {
        Component component;
        if (growthValue < 1) {
            component = JadeColors.colored(String.format(Locale.ROOT, "%.0f%%", growthValue * 100), JadeColors.INFO);
        } else {
            component = JadeColors.colored("Mature", JadeColors.SUCCESS);
        }
        tooltip.add(new TextElement(
                Component.literal("Growth: ").append(component),
                JadeColors.NORMAL,
                true));
    }
}
