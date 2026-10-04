package com.zephyr.client.module.qol.auramap;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static identity for the AuraMap code ported into Zephyr as a QoL module.
 * Replaces the standalone mod's entrypoint class (logging + identifier factory)
 * so the ported code has no dependency on a separate {@code auramap} mod id.
 */
public final class AuraMapBridge {
    public static final String MOD_ID = "zephyr";
    public static final Logger LOGGER = LoggerFactory.getLogger("AuraMap");

    private AuraMapBridge() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
