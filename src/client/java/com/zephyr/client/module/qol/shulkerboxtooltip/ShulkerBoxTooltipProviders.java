package com.zephyr.client.module.qol.shulkerboxtooltip;

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

        List<String> colorPrefixes = List.of(
                "white_", "orange_", "magenta_", "light_blue_", "yellow_", "lime_", "pink_", "gray_",
                "light_gray_", "cyan_", "purple_", "blue_", "brown_", "green_", "red_", "black_");
        List<String> copperPrefixes = List.of(
                "copper_", "exposed_copper_", "weathered_copper_", "oxidized_copper_",
                "waxed_copper_", "waxed_exposed_copper_", "waxed_weathered_copper_", "waxed_oxidized_copper_");

        List<Block> dyedShulkerBoxes = List.of(
                Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.MAGENTA_SHULKER_BOX,
                Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX,
                Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.LIGHT_GRAY_SHULKER_BOX,
                Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX,
                Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.BLACK_SHULKER_BOX);
        List<Block> copperChests = List.of(
                Blocks.COPPER_CHEST, Blocks.EXPOSED_COPPER_CHEST, Blocks.WEATHERED_COPPER_CHEST,
                Blocks.OXIDIZED_COPPER_CHEST, Blocks.WAXED_COPPER_CHEST, Blocks.WAXED_EXPOSED_COPPER_CHEST,
                Blocks.WAXED_WEATHERED_COPPER_CHEST, Blocks.WAXED_OXIDIZED_COPPER_CHEST);

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
