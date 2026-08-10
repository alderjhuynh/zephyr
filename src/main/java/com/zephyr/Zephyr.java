package com.zephyr;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Zephyr's mod initializer (Fabric entry point). Runs during the common
 * mod-load-ready phase of game startup. The actual client behavior lives in
 * {@code com.zephyr.client.ZephyrClient}; this class only defines the mod
 * identifier, shared logger, and a helper for namespaced resource identifiers.
 */
public class Zephyr implements ModInitializer {
	public static final String MOD_ID = "zephyr";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Called by Fabric once the game is in a mod-load-ready state. Currently a
	 * placeholder that logs a startup message; the real initialization is
	 * performed by the client initializer.
	 */
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");
	}

	/** Builds a namespaced resource identifier under the Zephyr mod id. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
