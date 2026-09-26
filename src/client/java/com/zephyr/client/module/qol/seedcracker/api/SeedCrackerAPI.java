package com.zephyr.client.module.qol.seedcracker.api;

/**
 * Public entry point for consuming seedcracker results.
 *
 * <p>Implementations are registered as entrypoints and receive the recovered world seed once the
 * {@code TimeMachine} has narrowed the candidate set down to a single seed.
 */
public interface SeedCrackerAPI {

    /**
     * Called when the cracker has successfully recovered a single world seed.
     *
     * @param seed the recovered world seed
     */
    void pushWorldSeed(long seed);

}
