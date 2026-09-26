package com.zephyr.client.module.qol.jade.provider;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;

/**
 * Resolves the mod that added a block or entity, mirroring Jade's
 * {@code ModIdentification}. "minecraft" maps to "Minecraft"; anything else looks
 * up the Fabric mod container by namespace and falls back to the raw namespace.
 */
public final class ModIdentification {
    /** Static utility; not instantiable. */
    private ModIdentification() {
    }

    /**
     * @param block the block to look up
     * @return the display name of the mod that registered the block
     */
    public static String getModName(Block block) {
        return getModName(BuiltInRegistries.BLOCK.getKey(block));
    }

    /**
     * @param entity the entity to look up
     * @return the display name of the mod that registered the entity type
     */
    public static String getModName(Entity entity) {
        return getModName(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    /**
     * Resolves a mod display name from a registry identifier's namespace.
     *
     * @param id the registry identifier of the block or entity type
     * @return the mod name, "Minecraft" for the vanilla namespace, or the raw
     *         namespace when no mod container is found
     */
    private static String getModName(Identifier id) {
        if (id == null) {
            return "Minecraft";
        }
        if ("minecraft".equals(id.getNamespace())) {
            return "Minecraft";
        }
        return FabricLoader.getInstance()
                .getModContainer(id.getNamespace())
                .map(container -> container.getMetadata().getName())
                .orElse(id.getNamespace());
    }
}
