package com.zephyr.client.module.qol.shulkerboxtooltip;

import com.zephyr.client.module.qol.shulkerboxtooltip.util.ShulkerBoxTooltipUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Registers a provider for a fixed set of blocks, all sharing the same inventory size.
 */
public record FixedPreviewProviderRegistry<I extends Container>(PreviewProviderRegistry registry,
                                                                BiFunction<Integer, Supplier<I>, PreviewProvider> providerFactory) {
    public FixedPreviewProviderRegistry<I> register(int maxRowSize, BiFunction<BlockPos, BlockState, I> inventoryFactory,
                                                    Block block) {
        var provider = providerFactory.apply(maxRowSize,
                () -> inventoryFactory.apply(BlockPos.ZERO, block.defaultBlockState()));
        registry.register(provider, block.asItem());
        return this;
    }

    public FixedPreviewProviderRegistry<I> registerCollection(int maxRowSize,
                                                             BiFunction<BlockPos, BlockState, I> inventoryFactory,
                                                             List<String> prefixes, List<Block> blocks) {
        ShulkerBoxTooltipUtil.zipApply(prefixes, blocks,
                (prefix, block) -> this.register(maxRowSize, inventoryFactory, block));
        return this;
    }
}
