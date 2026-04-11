package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

public class GhostHand {
    public static boolean enabled = true;

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Set<BlockPos> posList = new HashSet<>();

    public static void onItemUse() {
        if (!enabled || mc.player == null || mc.world == null) return;

        if (!mc.options.useKey.isPressed() || mc.player.isSneaking()) return;

        ClientPlayerEntity player = mc.player;

        BlockHitResult hit = (BlockHitResult) player.raycast(
                player.getBlockInteractionRange(),
                mc.getRenderTickCounter().getTickDelta(true),
                false
        );

        BlockPos hitPos = BlockPos.ofFloored(hit.getPos());
        if (mc.world.getBlockState(hitPos).hasBlockEntity()) return;
        
        Vec3d direction = new Vec3d(0, 0, 0.1)
                .rotateX(-(float) Math.toRadians(player.getPitch()))
                .rotateY(-(float) Math.toRadians(player.getYaw()));

        posList.clear();

        Vec3d cameraPos = player.getCameraPosVec(mc.getRenderTickCounter().getTickDelta(true));

        for (int i = 1; i < player.getBlockInteractionRange() * 10; i++) {
            BlockPos pos = BlockPos.ofFloored(cameraPos.add(direction.multiply(i)));

            if (!posList.add(pos)) continue;

            if (mc.world.getBlockState(pos).hasBlockEntity()) {
                for (Hand hand : Hand.values()) {
                    ActionResult result = mc.interactionManager.interactBlock(
                            player,
                            hand,
                            new BlockHitResult(
                                    new Vec3d(
                                            pos.getX() + 0.5,
                                            pos.getY() + 0.5,
                                            pos.getZ() + 0.5
                                    ),
                                    Direction.UP,
                                    pos,
                                    true
                            )
                    );

                    if (result.isAccepted() || result.shouldSwingHand()) {
                        player.swingHand(hand);
                        return;
                    }
                }
            }
        }
    }
}