package com.zephyr.client.commands;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of {@code ckit}. Stores kits in config/zephyr/kits.dat.
 */
public final class KitCommand extends Command {
    public static final KitCommand INSTANCE = new KitCommand();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("zephyr");
    private static final Path KITS_FILE = CONFIG_DIR.resolve("kits.dat");
    private static final Map<String, ListTag> kits = new HashMap<>();

    static {
        try { loadFile(); } catch (Exception e) { LOGGER.error("Failed to load kits", e); }
    }

    private KitCommand() {
        super("kit", "Kit management: .z kit <create|delete|edit|load|list|preview> <name> (alias: ckit)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("create", "delete", "edit", "load", "list", "preview");
        if (args.length == 2 && !args[0].equalsIgnoreCase("create") && !args[0].equalsIgnoreCase("list")) return List.copyOf(kits.keySet());
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) { CommandManager.sendMessage("Usage: .z kit <create|delete|edit|load|list|preview> ..."); return; }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> {
                if (args.length < 2) { CommandManager.sendMessage("Usage: .z kit create <name>"); return; }
                create(args[1]);
            }
            case "delete" -> {
                if (args.length < 2) { CommandManager.sendMessage("Usage: .z kit delete <name>"); return; }
                delete(args[1]);
            }
            case "edit" -> {
                if (args.length < 2) { CommandManager.sendMessage("Usage: .z kit edit <name>"); return; }
                edit(args[1]);
            }
            case "load" -> {
                if (args.length < 2) { CommandManager.sendMessage("Usage: .z kit load <name> [--override]"); return; }
                boolean override = args.length >= 3 && args[2].equals("--override");
                load(args[1], override);
            }
            case "list" -> list();
            case "preview" -> {
                if (args.length < 2) { CommandManager.sendMessage("Usage: .z kit preview <name>"); return; }
                preview(args[1]);
            }
            default -> CommandManager.sendMessage("Unknown kit subcommand: " + sub);
        }
    }

    private void create(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (kits.containsKey(name)) { CommandManager.sendMessage("Kit already exists: " + name); return; }
        var access = mc.level != null ? mc.level.registryAccess() : mc.getConnection().registryAccess();
        kits.put(name, saveInventory(access, mc.player.getInventory()));
        saveFile();
        CommandManager.sendMessage("Created kit '" + name + "'");
    }

    private void delete(String name) {
        if (kits.remove(name) == null) { CommandManager.sendMessage("Kit not found: " + name); return; }
        saveFile();
        CommandManager.sendMessage("Deleted kit '" + name + "'");
    }

    private void edit(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!kits.containsKey(name)) { CommandManager.sendMessage("Kit not found: " + name); return; }
        var access = mc.level != null ? mc.level.registryAccess() : mc.getConnection().registryAccess();
        kits.put(name, saveInventory(access, mc.player.getInventory()));
        saveFile();
        CommandManager.sendMessage("Edited kit '" + name + "'");
    }

    private void load(String name, boolean override) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        if (!mc.player.isCreative()) { CommandManager.sendMessage("You must be in creative to load kits"); return; }
        ListTag kit = kits.get(name);
        if (kit == null) { CommandManager.sendMessage("Kit not found: " + name); return; }
        var access = mc.level != null ? mc.level.registryAccess() : mc.getConnection().registryAccess();
        Inventory temp = new Inventory(mc.player, new EntityEquipment());
        loadInventory(access, temp, kit);
        var slots = mc.player.inventoryMenu.slots;
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            if (s.container == mc.player.getInventory()) {
                ItemStack stack = temp.getItem(s.getContainerSlot());
                if (!stack.isEmpty() || override) {
                    mc.player.getInventory().setItem(s.getContainerSlot(), stack);
                    mc.gameMode.handleCreativeModeItemAdd(stack, i);
                }
            }
        }
        mc.player.inventoryMenu.broadcastChanges();
        CommandManager.sendMessage("Loaded kit '" + name + "'");
    }

    private void list() {
        if (kits.isEmpty()) CommandManager.sendMessage("No kits");
        else CommandManager.sendMessage("Kits: " + String.join(", ", kits.keySet()));
    }

    private void preview(String name) {
        CommandManager.sendMessage("Preview for '" + name + "' not implemented in this port. Use load to view.");
    }

    private void saveFile() {
        try {
            CompoundTag root = new CompoundTag();
            CompoundTag ct = new CompoundTag();
            for (var e : kits.entrySet()) ct.put(e.getKey(), e.getValue());
            root.putInt("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
            root.put("Kits", ct);
            Files.createDirectories(CONFIG_DIR);
            Path newFile = File.createTempFile("kits", ".dat", CONFIG_DIR.toFile()).toPath();
            NbtIo.write(root, newFile);
            Path backup = CONFIG_DIR.resolve("kits.dat_old");
            Path cur = CONFIG_DIR.resolve("kits.dat");
            Util.safeReplaceFile(cur, newFile, backup);
        } catch (Exception e) { CommandManager.sendMessage("Failed to save kits: " + e.getMessage()); }
    }

    private static void loadFile() throws Exception {
        if (!Files.exists(KITS_FILE)) return;
        CompoundTag root = NbtIo.read(KITS_FILE);
        if (root == null) return;
        CompoundTag ct = root.getCompoundOrEmpty("Kits");
        for (String key : ct.keySet()) {
            kits.put(key, ct.getListOrEmpty(key));
        }
    }

    private static void loadInventory(HolderLookup.Provider reg, Inventory inv, ListTag items) {
        CompoundTag nbt = new CompoundTag();
        nbt.put(ContainerHelper.TAG_ITEMS, items);
        try (var collector = new net.minecraft.util.ProblemReporter.ScopedCollector(LOGGER)) {
            var input = TagValueInput.create(collector, reg, nbt);
            inv.load(input.listOrEmpty(ContainerHelper.TAG_ITEMS, ItemStackWithSlot.CODEC));
        }
    }

    private static ListTag saveInventory(HolderLookup.Provider reg, Inventory inv) {
        try (var collector = new net.minecraft.util.ProblemReporter.ScopedCollector(LOGGER)) {
            var output = TagValueOutput.createWithContext(collector, reg);
            inv.save(output.list(ContainerHelper.TAG_ITEMS, ItemStackWithSlot.CODEC));
            return output.buildResult().getListOrEmpty(ContainerHelper.TAG_ITEMS);
        }
    }
}
