package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.ListSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public final class Xray extends Module {
    public static final Xray INSTANCE = new Xray();

    private static final Map<Block, Integer> BLOCKS = Map.ofEntries(
            Map.entry(Blocks.COAL_ORE, 0xFF2a2a2b),
            Map.entry(Blocks.DEEPSLATE_COAL_ORE, 0xFF2a2a2b),
            Map.entry(Blocks.IRON_ORE, 0xFF88817B),
            Map.entry(Blocks.DEEPSLATE_IRON_ORE, 0xFF88817B),
            Map.entry(Blocks.GOLD_ORE, 0xFF91866b),
            Map.entry(Blocks.DEEPSLATE_GOLD_ORE, 0xFF91866b),
            Map.entry(Blocks.DIAMOND_ORE, 0xFF5cdbd5),
            Map.entry(Blocks.DEEPSLATE_DIAMOND_ORE, 0xFF5cdbd5),
            Map.entry(Blocks.EMERALD_ORE, 0xFF2acb58),
            Map.entry(Blocks.DEEPSLATE_EMERALD_ORE, 0xFF2acb58),
            Map.entry(Blocks.COPPER_ORE, 0xFFC06C50),
            Map.entry(Blocks.DEEPSLATE_COPPER_ORE, 0xFFC06C50),
            Map.entry(Blocks.REDSTONE_ORE, 0xFFC32C1F),
            Map.entry(Blocks.DEEPSLATE_REDSTONE_ORE, 0xFFC32C1F),
            Map.entry(Blocks.LAPIS_ORE, 0xFF1F438C),
            Map.entry(Blocks.DEEPSLATE_LAPIS_ORE, 0xFF1F438C),
            Map.entry(Blocks.NETHER_QUARTZ_ORE, 0xFFFFFCF5),
            Map.entry(Blocks.NETHER_GOLD_ORE, 0xFF91866b),
            Map.entry(Blocks.ANCIENT_DEBRIS, 0xFFff0000)
    );

    private final EnumSetting<Xray.BlockType> block = new EnumSetting<>("Mode", Xray.BlockType.COAL);
    public enum BlockType {
        COAL,
        IRON,
        GOLD,
        DIAMOND,
        EMERALD,
        COPPER,
        REDSTONE,
        LAPIS,
        QUARTZ,
        DEBRIS,
        LIST
    }

    private final NumberSetting searchRadius =
            new NumberSetting("Search Radius", 24, 1, 30, 1);

    private final ListSetting customBlocks = new ListSetting("Custom Blocks");

    private Xray() {
        super("Xray", "Outlines blocks in a list through walls", Category.QOL);
        addSetting(searchRadius);
        addSetting(block);
        addSetting(customBlocks);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled()) {
            return;
        }

        if (client.level == null || client.player == null) {
            return;
        }

        int SEARCH_RADIUS = (int) Math.round(searchRadius.get());

        BlockPos center = client.player.blockPosition();
        BlockPos min = center.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS);
        BlockPos max = center.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS);

        try (var ignored = client.collectPerTickGizmos()) {
            if (block.get() == Xray.BlockType.LIST) {
                renderCustom(client, min, max);
            } else {
                renderPreset(client, min, max);
            }
        }
    }

    private void renderPreset(Minecraft client, BlockPos min, BlockPos max) {
        BlockPos.betweenClosedStream(min, max).forEach(pos -> {
            BlockState state = client.level.getBlockState(pos);

            String path = BuiltInRegistries.BLOCK
                    .getKey(state.getBlock())
                    .getPath();

            if (!path.toLowerCase().contains(block.get().name().toLowerCase())) {
                return;
            }

            Integer color = BLOCKS.get(state.getBlock());
            if (color != null) {
                Gizmos.cuboid(pos.immutable(), GizmoStyle.stroke(color))
                        .setAlwaysOnTop();
            }
        });
    }

    private void renderCustom(Minecraft client, BlockPos min, BlockPos max) {
        Map<Block, Integer> colors = new HashMap<>();
        for (ListSetting.ListEntry entry : customBlocks.get()) {
            if (entry.blockName().isBlank()) continue;
            Identifier id = entry.blockName().contains(":")
                    ? Identifier.tryParse(entry.blockName())
                    : Identifier.tryParse("minecraft:" + entry.blockName());
            if (id == null) continue;
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block == Blocks.AIR) continue;
            colors.put(block, ListSetting.parseColor(entry.color(), 0xFFFFFFFF));
        }
        if (colors.isEmpty()) return;

        BlockPos.betweenClosedStream(min, max).forEach(pos -> {
            BlockState state = client.level.getBlockState(pos);
            Integer color = colors.get(state.getBlock());
            if (color != null) {
                Gizmos.cuboid(pos.immutable(), GizmoStyle.stroke(color))
                        .setAlwaysOnTop();
            }
        });
    }
}
