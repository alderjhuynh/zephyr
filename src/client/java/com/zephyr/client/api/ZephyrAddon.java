package com.zephyr.client.api;

/**
 * Entrypoint interface for Zephyr addons. Implement this and declare the class as a
 * {@code "zephyr"} entrypoint in your {@code fabric.mod.json}:
 * <pre>{@code
 * "entrypoints": {
 *   "zephyr": [ "com.example.myaddon.MyAddon" ]
 * }
 * }</pre>
 * It is invoked on the client thread after Zephyr has initialized its config, modules,
 * profiles and keybinds - the right time to call {@link ZephyrApi#registerModule},
 * {@link ZephyrApi#registerCategory}, {@link ZephyrApi#registerCommand},
 * {@link ZephyrApi#registerCycleScreen} and {@link ZephyrEvents} registrations.
 * <p>
 * Addons may also skip this and call the API from their own
 * {@code ClientModInitializer}; registration is safe at any point during startup.
 */
public interface ZephyrAddon {

    /** Called once during client initialization, after Zephyr is fully set up. */
    void onInitializeZephyr();
}
