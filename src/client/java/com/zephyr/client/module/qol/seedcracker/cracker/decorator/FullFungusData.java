package com.zephyr.client.module.qol.seedcracker.cracker.decorator;

import com.seedfinding.latticg.reversal.DynamicProgram;
import com.seedfinding.latticg.reversal.calltype.java.JavaCalls;
import com.seedfinding.latticg.util.LCG;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

/**
 * Complete structural snapshot of a single huge warped fungus, used to reverse its RNG.
 *
 * <p>Captures the fungus's layer sizes, per-layer block types, vine data and big-trunk data. The
 * {@link #crackSeed()} method encodes all observed randomness into a {@link DynamicProgram} and
 * reverses it to recover the decorator seed(s) that produce the observed fungus.
 */
public class FullFungusData {


    /** Height (in blocks) of each fungus layer, bottom to top. */
    public final ArrayList<Integer> layerSizes = new ArrayList<>();
    /** Block type of every position within each layer (see the blockType codes in {@link #crackSeed()}). */
    public final int[][][] layers;
    /** Vine heights around the upper layers; 0 marks positions with no vine. */
    public final ArrayList<Integer> vines = new ArrayList<>();
    /** Per-position trunk block data (1 = stem block present, 0 = not) for big fungi. */
    public final ArrayList<Integer> bigtrunkData = new ArrayList<>();
    /** Estimated number of random calls this fungus constrains; used to pick the best candidate. */
    public final int estimatedData;
    /** Whether this is a big (4-block trunk) fungus. */
    public boolean big;
    /** Total height of the fungus trunk. */
    public int height;
    /** Radius of the vine layer. */
    public int vineLayerSize;

    public FullFungusData(List<Integer> layerSizes, int[][][] layers, ArrayList<Integer> vines, boolean big, int height, int vineLayerSize, ArrayList<Integer> bigTrunkData, int estimatedData) {
        this.layerSizes.addAll(layerSizes);
        this.layers = layers.clone();
        this.vines.addAll(vines);
        this.big = big;
        this.height = height;
        this.vineLayerSize = vineLayerSize;
        this.bigtrunkData.addAll(bigTrunkData);
        this.estimatedData = estimatedData;
    }


    /**
     * Picks the fungus whose structure constrains the most random bits.
     *
     * @param fungusList the observed fungi to choose from
     * @return the fungus with the highest {@link #estimatedData}, or null if the list is empty
     */
    public static FullFungusData getBestFungus(List<FullFungusData> fungusList) {
        int data = 0;
        FullFungusData out = null;

        for (FullFungusData fungus : fungusList) {
            int fungusData = fungus.estimatedData;
            if (fungusData > data) {
                data = fungusData;
                out = fungus;
            }
        }

        return out;
    }

    /**
     * Reverses the observed fungus structure into candidate decorator seeds.
     *
     * <p>Builds a {@link DynamicProgram} describing every random call that shaped this fungus (vine
     * heights, trunk growth, layer blocks) and reverses it.
     *
     * @return a stream of decorator seeds consistent with the observed fungus
     */
    public LongStream crackSeed() {
        int doppelt = 0;
        if (height > 7 && height % 2 == 0) {
            doppelt = 1;
            if (height > 13) {
                doppelt = 2;
            }
        }

        DynamicProgram device = DynamicProgram.create(LCG.JAVA);
        if (doppelt < 2) {
            device.skip(2);
        } else {
            device.skip(1);
            device.add(JavaCalls.nextInt(12).equalTo(0));
        }
        if (big) {
            device.add(JavaCalls.nextFloat().betweenII(0F, 0.06F));
        } else {
            device.skip(1);
        }

        if (big) {
            for (int blockdata : bigtrunkData) {
                if (blockdata == 0) {
                    device.skip(1);
                } else if (blockdata == 1) {
                    device.add(JavaCalls.nextFloat().betweenII(0F, 0.1F));
                }
            }
        }

        device.skip(2);

        ArrayList<Integer> done = new ArrayList<>();
        for (int j = 3; j > 0; j--) {

            for (int i = 0; i < vineLayerSize * 8; i++) {
                if (vines.get(i) == 0 && !done.contains(i)) {
                    device.skip(1);

                } else if (vines.get(i) == j) {
                    done.add(i);
                    device.add(JavaCalls.nextFloat().betweenII(0F, 0.15F));

                } else if (!done.contains(i)) {
                    device.skip(1);

                }
            }

            device.skip(1);
        }
        int relativePos;
        int blockType;
        int layer = 0;

        for (int size : layerSizes) {
            size *= 2;

            for (int x = 0; x <= size; x++) {

                boolean siteX = x == 0 || x == size;
                for (int z = 0; z <= size; z++) {

                    boolean siteZ = z == 0 || z == size;

                    relativePos = (siteX ? 1 : 0) + (siteZ ? 1 : 0);

                    blockType = layers[layer][x][z];

                    generateBlock(relativePos, blockType, device);
                }
            }

            device.skip(1);
            layer++;
        }
        return device.reverse();
    }

    private void generateBlock(int relativePos, int blockType, DynamicProgram device) {

        if (blockType == 3) return;
        switch (relativePos) {
            case 0:
                //Inside
                switch (blockType) {
                    case 0:
                        device.skip(2);
                        break;
                    case 1:
                        device.skip(3);
                        break;
                    case 2:
                        device.add(JavaCalls.nextFloat().betweenII(0F, 0.1F));
                }
                break;
            case 1:
                //Wall
                switch (blockType) {
                    case 0:
                        device.skip(1);
                        device.add(JavaCalls.nextFloat().betweenII(0.98F, 1F));
                        break;
                    case 1:
                        device.skip(3);
                        break;
                    case 2:
                        device.add(JavaCalls.nextFloat().betweenII(0F, 5.0E-4F));
                }
                break;
            case 2:
                //Corner
                switch (blockType) {
                    case 0:
                        device.skip(2);
                        break;
                    case 1:
                        device.skip(3);
                        break;
                    case 2:
                        device.add(JavaCalls.nextFloat().betweenII(0F, 0.01F));
                }
                break;
        }
    }
}
