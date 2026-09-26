package com.zephyr.client.module.qol.seedcracker.cracker;

import com.seedfinding.mccore.rand.seed.WorldSeed;
import com.seedfinding.mcseed.rand.JRand;

/**
 * Holds a hashed world seed constraint.
 *
 * <p>When the server's world seed hash is known (e.g. inferred from biome hints), this constraint
 * is used to recover the actual seed by brute-forcing over the 48-bit hashed seed space.
 */
public class HashedSeedData {

    private final long hashedSeed;

    public HashedSeedData(long hashedSeed) {
        this.hashedSeed = hashedSeed;
    }

    /**
     * Checks whether the given seed hashes to the recorded hashed seed.
     *
     * @param seed the candidate world seed
     * @param rand unused scratch random, kept for interface symmetry
     * @return true if {@code WorldSeed.toHash(seed)} equals the stored hash
     */
    public boolean test(long seed, JRand rand) {
        return WorldSeed.toHash(seed) == this.hashedSeed;
    }

    /**
     * @return the hashed world seed this constraint wraps
     */
    public long getHashedSeed() {
        return this.hashedSeed;
    }

}
