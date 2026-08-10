package com.zephyr.client.module.qol.shulkerboxtooltip;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.entity.*;

import java.util.List;

/**
 * Registers the preview providers for every supported container item.
 */
public final class ShulkerBoxTooltipProviders {
    /** Static utility; not instantiable. */
    private ShulkerBoxTooltipProviders() {
    }

    /** Registers a preview provider for every supported container item. */
    public static void register() {
        PreviewProviderRegistry registry = PreviewProviderRegistry.getInstance();

        List<String> colorPrefixes = ColorCollection.NAMES.map(n -> n + "_").asList();
        List<String> copperPrefixes = WeatheringCopperCollection.PREFIXES.asList();

        List<Block> dyedShulkerBoxes = Blocks.DYED_SHULKER_BOX.asList();
        List<Block> copperChests = Blocks.COPPER_CHEST.asList();

        new FixedPreviewProviderRegistry<>(registry, ShulkerBoxPreviewProvider::new)
                .register(9, ShulkerBoxBlockEntity::new, Blocks.SHULKER_BOX)
                .registerCollection(9, ShulkerBoxBlockEntity::new, colorPrefixes, dyedShulkerBoxes);

        new FixedPreviewProviderRegistry<>(registry, InventoryAwarePreviewProvider::new)
                .register(9, ChestBlockEntity::new, Blocks.CHEST)
                .registerCollection(9, ChestBlockEntity::new, copperPrefixes, copperChests)
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
                .register(1, DecoratedPotBlockEntity::new, Blocks.DECORATED_POT)
                .register(3, ShelfBlockEntity::new, Blocks.ACACIA_SHELF)
                .register(3, ShelfBlockEntity::new, Blocks.BAMBOO_SHELF)
                .register(3, ShelfBlockEntity::new, Blocks.BIRCH_SHELF)
                .register(3, ShelfBlockEntity::new, Blocks.CHERRY_SHELF)
                .register(3, ShelfBlockEntity::new, Blocks.CRIMSON_SHELF)
                .register(3, ShelfBlockEntity::new, Blocks.DARK_OAK_SHELF)
                .register(3, ShelfBlockEntity::new, Blocks.JUNGLE_SHELF)
                .register(3, ShelfBlockEntity::new, Blocks.MANGROVE_SHELF)
                .register(3, ShelfBlockEntity::new, Blocks.OAK_SHELF)
                .register(3, ShelfBlockEntity::new, Blocks.PALE_OAK_SHELF)
                .register(3, ShelfBlockEntity::new, Blocks.SPRUCE_SHELF)
                .register(3, ShelfBlockEntity::new, Blocks.WARPED_SHELF);
    }
}
