package com.zephyr.client.module.qol.seedcracker.finder.decorator;

import com.seedfinding.mcfeature.decorator.EndGateway;
import com.zephyr.client.module.qol.seedcracker.Features;
import com.zephyr.client.module.qol.Seedcracker;
import com.zephyr.client.module.qol.seedcracker.cracker.DataAddedEvent;
import com.zephyr.client.module.qol.seedcracker.finder.BlockFinder;
import com.zephyr.client.module.qol.seedcracker.finder.Finder;
import com.zephyr.client.module.qol.seedcracker.render.Cuboid;
import com.zephyr.client.module.qol.seedcracker.util.BiomeFixer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds end gateways in the End by scanning for {@link Blocks#END_GATEWAY} blocks.
 *
 * <p>Gateways are only credited when their height above the end stone is between 3 and 9 blocks,
 * which is the range matching a legitimately generated gateway.
 */
public class EndGatewayFinder extends BlockFinder {

    public EndGatewayFinder(Level world, ChunkPos chunkPos) {
        super(world, chunkPos, Blocks.END_GATEWAY);
        this.searchPositions = CHUNK_POSITIONS;
    }

    /**
     * @return a single {@link EndGatewayFinder} for the given chunk
     */
    public static List<Finder> create(Level world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new EndGatewayFinder(world, chunkPos));
        return finders;
    }

    /**
     * Scans for gateway blocks and records an {@code EndGateway.Data} constraint for those at a
     * valid height.
     *
     * @return the matched gateway positions
     */
    @Override
    public List<BlockPos> findInChunk() {
        Biome biome = this.world.getNoiseBiome((this.chunkPos.x() << 2) + 2, 64, (this.chunkPos.z() << 2) + 2).value();
        if(!Features.END_GATEWAY.isValidBiome(BiomeFixer.swap(biome)))return new ArrayList<>();

        List<BlockPos> result = super.findInChunk();
        List<BlockPos> newResult = new ArrayList<>();

        result.forEach(pos -> {
            int height = this.findHeight(pos);

            if (height >= 3 && height <= 9) {
                newResult.add(pos);

                EndGateway.Data data = Features.END_GATEWAY.at(pos.getX(), pos.getZ(), height);

                if (Seedcracker.get().getDataStorage().addBaseData(data, DataAddedEvent.POKE_STRUCTURES)) {
                    this.cuboids.add(new Cuboid(pos.offset(-1, -2, -1), pos.offset(2, 3, 2), ARGB.color(102, 102, 210)));
                }
            }
        });

        return newResult;
    }

    /**
     * Measures the number of end-stone blocks directly below a gateway position.
     *
     * @param pos the gateway position
     * @return the height above the first non-end-stone block, minus one
     */
    private int findHeight(BlockPos pos) {
        int height = 0;

        while (pos.getY() >= 0) {
            pos = pos.below();
            height++;

            BlockState state = this.world.getBlockState(pos);

            //Bedrock generates below gateways.
            if (state.getBlock() == Blocks.BEDROCK || state.getBlock() != Blocks.END_STONE) {
                continue;
            }

            break;
        }

        return height - 1;
    }

    /**
     * @return true for the End dimension
     */
    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isEnd(dimension);
    }

}
