package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
import com.zephyr.client.configplusgui.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class ContainerESP extends Module {
    public static final ContainerESP INSTANCE = new ContainerESP();

    private static final Map<Block, Integer> BLOCKS = Map.ofEntries(
            Map.entry(Blocks.CHEST, 0xFF6e3c1a),
            Map.entry(Blocks.TRAPPED_CHEST, 0xFFa81e14),
            Map.entry(Blocks.BARREL, 0xFF6e3c1a),
            Map.entry(Blocks.SHULKER_BOX, 0xFF941694),
            Map.entry(Blocks.ENDER_CHEST, 0xFFbf05fc)
    );

    private final NumberSetting searchRadius =
            new NumberSetting("Search Radius", 24, 1, 30, 1);

    private ContainerESP() {
        super("Container ESP", "Outlines containers", Category.QOL);
        addSetting(searchRadius);
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
            BlockPos.betweenClosedStream(min, max).forEach(pos -> {
                BlockState state = client.level.getBlockState(pos);

                Block block = state.getBlock();

                Integer color = BLOCKS.get(block);

                if (color == null) {
                    String path = BuiltInRegistries.BLOCK.getKey(block).getPath();

                    if (path.endsWith("shulker_box")) {
                        color = 0xFF941694;
                    }
                }

                if (color != null) {
                    Gizmos.cuboid(pos.immutable(), GizmoStyle.stroke(color))
                            .setAlwaysOnTop();
                }

                if (color != null) {
                    Gizmos.cuboid(pos.immutable(), GizmoStyle.stroke(color))
                            .setAlwaysOnTop();
                }
            });
        }
    }
}