package com.zephyr.client.module.qol.seedcracker.cracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Observed end pillar height ordering used to recover the 16-bit pillar seed.
 *
 * <p>The heights of the ten end pillars are determined by shuffling the indices 0..9 with a
 * {@link Random} seeded by the pillar seed. Comparing observed heights against the generated
 * ordering reduces the space of possible pillar seeds.
 */
public class PillarData {

    private final List<Integer> heights;

    public PillarData(List<Integer> heights) {
        this.heights = heights;
    }

    /**
     * Checks whether the pillar heights generated from the given seed match the observed heights.
     *
     * @param seed the candidate pillar seed
     * @return true if the observed height order matches
     */
    public boolean test(long seed) {
        List<Integer> h = this.getPillarHeights((int) seed);
        return h.equals(this.heights);
    }

    /**
     * Computes the end pillar heights for the given pillar seed.
     *
     * @param pillarSeed the 16-bit pillar seed
     * @return the ordered list of ten pillar heights (from 76 to 103)
     */
    public List<Integer> getPillarHeights(int pillarSeed) {
        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            indices.add(i);
        }

        Collections.shuffle(indices, new Random(pillarSeed));

        List<Integer> heights = new ArrayList<>();

        for (Integer index : indices) {
            heights.add(76 + index * 3);
        }

        return heights;
    }

}
