package com.zephyr.client.module.qol.shulkerboxtooltip;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;

import java.util.List;

/**
 * Registers the preview providers for every supported container item.
 */
public final class ShulkerBoxTooltipProviders {
    private ShulkerBoxTooltipProviders() {
    }

    public static void register() {
        PreviewProviderRegistry registry = PreviewProviderRegistry.getInstance();

        List<Block> shulkerBoxes = List.of(
                Blocks.SHULKER_BOX,
                Blocks.WHITE_SHULKER_BOX,
                Blocks.ORANGE_SHULKER_BOX,
                Blocks.MAGENTA_SHULKER_BOX,
                Blocks.LIGHT_BLUE_SHULKER_BOX,
                Blocks.YELLOW_SHULKER_BOX,
                Blocks.LIME_SHULKER_BOX,
                Blocks.PINK_SHULKER_BOX,
                Blocks.GRAY_SHULKER_BOX,
                Blocks.LIGHT_GRAY_SHULKER_BOX,
                Blocks.CYAN_SHULKER_BOX,
                Blocks.PURPLE_SHULKER_BOX,
                Blocks.BLUE_SHULKER_BOX,
                Blocks.BROWN_SHULKER_BOX,
                Blocks.GREEN_SHULKER_BOX,
                Blocks.RED_SHULKER_BOX,
                Blocks.BLACK_SHULKER_BOX);
        new FixedPreviewProviderRegistry<>(registry, ShulkerBoxPreviewProvider::new)
                .registerAll(9, ShulkerBoxBlockEntity::new, shulkerBoxes);

        FixedPreviewProviderRegistry<Container> providers = new FixedPreviewProviderRegistry<>(registry,
                InventoryAwarePreviewProvider::new);

        providers.register(9, ChestBlockEntity::new, Blocks.CHEST)
                .register(9, TrappedChestBlockEntity::new, Blocks.TRAPPED_CHEST)
                .register(9, BarrelBlockEntity::new, Blocks.BARREL)
                .register(3, FurnaceBlockEntity::new, Blocks.FURNACE)
                .register(3, BlastFurnaceBlockEntity::new, Blocks.BLAST_FURNACE)
                .register(3, SmokerBlockEntity::new, Blocks.SMOKER)
                .register(3, DropperBlockEntity::new, Blocks.DROPPER)
                .register(3, DispenserBlockEntity::new, Blocks.DISPENSER)
                .register(5, HopperBlockEntity::new, Blocks.HOPPER)
                .register(5, BrewingStandBlockEntity::new, Blocks.BREWING_STAND)
                .register(3, ChiseledBookShelfBlockEntity::new, Blocks.CHISELED_BOOKSHELF)
                .register(1, DecoratedPotBlockEntity::new, Blocks.DECORATED_POT);
    }
}
