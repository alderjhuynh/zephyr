package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Port of {@code cfind}. Searches loaded entities by type or name.
 */
// 1.21.1 port: Identifier -> ResourceLocation, ENTITY_TYPE.getValue -> ENTITY_TYPE.get.
public final class FindCommand extends Command {
    public static final FindCommand INSTANCE = new FindCommand();

    private FindCommand() {
        super("find", "Find entities: .z find <type|name> (alias: cfind)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            return BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(ResourceLocation::toString).toList();
        }
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            CommandManager.sendMessage("You must be in a world");
            return;
        }
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z find <entity_type|name>");
            return;
        }
        String filter = String.join(" ", args).toLowerCase();
        // try to parse as entity type
        EntityType<?> type = null;
        ResourceLocation id = filter.contains(":") ? ResourceLocation.tryParse(filter) : ResourceLocation.tryParse("minecraft:" + filter);
        if (id != null) {
            type = BuiltInRegistries.ENTITY_TYPE.get(id);
            // if not found, treat as name filter
            if (type != null && filter.contains(":")) {
                // keep as type filter
            } else if (type != null && BuiltInRegistries.ENTITY_TYPE.getKey(type).toString().equals(id.toString())) {
                // valid type
            } else {
                // fallback: if filter was single word without colon, we already found type, use it
                // otherwise treat as name
                if (!filter.contains(":") && type != null) {
                    // keep
                } else if (filter.contains(":") && type == BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse("minecraft:" + filter))) {
                    // not valid
                    type = null;
                }
            }
        }
        // simpler: if filter exactly matches a registry key, use type else name
        // reset logic: check exact match
        ResourceLocation exact = ResourceLocation.tryParse(filter);
        if (exact != null && BuiltInRegistries.ENTITY_TYPE.containsKey(exact)) {
            type = BuiltInRegistries.ENTITY_TYPE.get(exact);
        } else {
            ResourceLocation mcId = ResourceLocation.tryParse("minecraft:" + filter);
            if (mcId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(mcId) && !filter.contains(" ")) {
                type = BuiltInRegistries.ENTITY_TYPE.get(mcId);
            } else {
                type = null;
            }
        }

        List<? extends Entity> entities;
        if (type != null) {
            EntityType<?> finalType = type;
            entities = mc.level.getEntities((Entity) null, mc.player.getBoundingBox().inflate(512), e -> e.getType() == finalType);
            // also include far entities not in box by scanning all
            if (entities.isEmpty()) {
                entities = mc.level.getEntities((Entity) null, mc.player.getBoundingBox().inflate(1000), e -> e.getType() == finalType);
            }
            // fallback to global scan
            if (entities.isEmpty()) {
                entities = java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false).filter(e -> e.getType() == finalType).toList();
            }
        } else {
            String lname = filter;
            entities = java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                    .filter(e -> e.getName().getString().toLowerCase().contains(lname) || e.getType().getDescription().getString().toLowerCase().contains(lname))
                    .toList();
        }

        if (entities.isEmpty()) {
            CommandManager.sendMessage("No entities found for: " + filter);
            return;
        }
        CommandManager.sendMessage("Found " + entities.size() + " entities for '" + filter + "':");
        for (Entity e : entities) {
            double dist = Math.sqrt(e.distanceToSqr(mc.player.position()));
            String pos = String.format("%d %d %d", e.blockPosition().getX(), e.blockPosition().getY(), e.blockPosition().getZ());
            CommandManager.sendMessage("- " + e.getName().getString() + " [" + e.getType().getDescription().getString() + "] at " + pos + " (" + String.format("%.1f", dist) + "m)");
        }
    }
}
