package com.zephyr.client.module.qol.seedcracker.finder;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Queues fake block-dig packets used to force the server to re-send unloaded blocks.
 *
 * <p>Used by the anti-x-ray bypass: the client aborts breaking blocks at floor positions so the
 * server transmits their states, letting {@link DungeonFinder} re-read the dungeon floor after
 * the blocks have been sent.
 */
public class BlockUpdateQueue {
    private final Queue<Pair<Thread, ArrayList<BlockPos>>> blocksAndAction = new LinkedList<>();
    private final HashSet<BlockPos> alreadyChecked = new HashSet<>();

    /**
     * Enqueues a set of blocks to poke, each origin position only once.
     *
     * @param blockPoses the block positions to poke
     * @param originPos the origin this batch belongs to (deduplication key)
     * @param operationAtEnd the thread to start once all blocks in the batch have been poked
     * @return true if this origin was newly queued
     */
    public boolean add(ArrayList<BlockPos> blockPoses, BlockPos originPos, Thread operationAtEnd) {
        if (alreadyChecked.add(originPos)) {
            blocksAndAction.add(new Pair<>(operationAtEnd, blockPoses));
            return true;
        }
        return false;
    }

    /**
     * Sends up to five block-abort packets per call, starting the queued operation thread once a
     * batch has been fully poked.
     */
    public void tick() {
        if (blocksAndAction.isEmpty()) return;

        Pair<Thread, ArrayList<BlockPos>> current = blocksAndAction.peek();
        ArrayList<BlockPos> currentBlocks = current.getSecond();
        for (int i = 0; i < 5; i++) {
            if (currentBlocks.isEmpty()) {
                current.getFirst().start();
                blocksAndAction.remove();
                if (blocksAndAction.isEmpty()) {
                    return;
                } else {
                    current = blocksAndAction.peek();
                    currentBlocks = current.getSecond();
                }
            }
            if (Minecraft.getInstance().getConnection() == null) {
                blocksAndAction.clear();
                return;
            }
            ServerboundPlayerActionPacket p = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, currentBlocks.remove(0),
                    Direction.DOWN);
            Minecraft.getInstance().getConnection().send(p);
        }
    }
}
