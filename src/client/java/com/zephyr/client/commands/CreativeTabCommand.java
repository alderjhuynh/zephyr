package com.zephyr.client.commands;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Port of {@code ccreativetab}. Stores tabs in config/zephyr/creative_tabs.dat similar to original.
 * Supports add/remove/modify. Requires restart after changes (tabs registered at startup via static init).
 */
public final class CreativeTabCommand extends Command {
    public static final CreativeTabCommand INSTANCE = new CreativeTabCommand();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("zephyr");
    private static final Path CREATIVE_TABS_FILE = CONFIG_DIR.resolve("creative_tabs.dat");
    private static final Map<String, Tab> tabs = new HashMap<>();

    static {
        try { loadFile(); } catch (Exception e) { LOGGER.error("Failed to load creative tabs", e); }
        try {
            var builtin = new net.minecraft.core.RegistryAccess.ImmutableRegistryAccess(BuiltInRegistries.REGISTRY.stream().toList());
            for (var e : tabs.entrySet()) {
                try { e.getValue().registerCreativeTab(builtin, e.getKey()); } catch (Throwable t) { LOGGER.error("Failed to register tab {}", e.getKey(), t); }
            }
        } catch (Throwable t) { LOGGER.error("Failed to register creative tabs", t); }
    }

    private CreativeTabCommand() {
        super("creativetab", "Manage custom creative tabs: .z creativetab <add|remove|modify ...> (alias: ccreativetab)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("add", "remove", "modify");
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) return List.copyOf(tabs.keySet());
        if (args.length == 2 && args[0].equalsIgnoreCase("modify")) return List.copyOf(tabs.keySet());
        if (args.length == 3 && args[0].equalsIgnoreCase("modify")) return List.of("add", "remove", "set", "icon", "rename");
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            CommandManager.sendMessage("Usage: .z creativetab <add <tab> <iconItem>|remove <tab>|modify <tab> <add|remove|set|icon|rename> ...>");
            return;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "add" -> {
                if (args.length < 3) { CommandManager.sendMessage("Usage: .z creativetab add <tab> <iconItem>"); return; }
                addTab(args[1], parseStack(args[2]));
            }
            case "remove" -> {
                if (args.length < 2) { CommandManager.sendMessage("Usage: .z creativetab remove <tab>"); return; }
                removeTab(args[1]);
            }
            case "modify" -> handleModify(java.util.Arrays.copyOfRange(args, 1, args.length));
            default -> CommandManager.sendMessage("Unknown subcommand: " + sub);
        }
    }

    private void handleModify(String[] a) {
        if (a.length < 2) { CommandManager.sendMessage("Usage: .z creativetab modify <tab> <add|remove|set|icon|rename> ..."); return; }
        String tab = a[0]; String op = a[1].toLowerCase();
        if (!tabs.containsKey(tab)) { CommandManager.sendMessage("Tab not found: " + tab); return; }
        switch (op) {
            case "add" -> {
                if (a.length < 3) { CommandManager.sendMessage("Usage: .z creativetab modify <tab> add <item>"); return; }
                Tab t = tabs.get(tab);
                HolderLookup.Provider prov = getRegistryAccess();
                ItemStack stack = parseStack(a[2]);
                if (stack.isEmpty()) { CommandManager.sendMessage("Unknown item: " + a[2]); return; }
                try {
                    t.items().add(saveStack(prov, stack));
                    saveFile();
                    CommandManager.sendMessage("Added " + a[2] + " to tab " + tab + ". Restart required.");
                } catch (Exception e) { CommandManager.sendMessage("Failed: " + e.getMessage()); }
            }
            case "remove" -> {
                if (a.length < 3) { CommandManager.sendMessage("Usage: .z creativetab modify <tab> remove <index>"); return; }
                try {
                    int idx = Integer.parseInt(a[2]);
                    Tab t = tabs.get(tab);
                    if (idx < 0 || idx >= t.items().size()) { CommandManager.sendMessage("Index out of bounds"); return; }
                    t.items().remove(idx);
                    saveFile();
                    CommandManager.sendMessage("Removed index " + idx + " from " + tab + ". Restart required.");
                } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid index"); } catch (Exception e) { CommandManager.sendMessage("Failed: " + e.getMessage()); }
            }
            case "set" -> {
                if (a.length < 4) { CommandManager.sendMessage("Usage: .z creativetab modify <tab> set <index> <item>"); return; }
                try {
                    int idx = Integer.parseInt(a[2]);
                    Tab t = tabs.get(tab);
                    if (idx < 0 || idx >= t.items().size()) { CommandManager.sendMessage("Index out of bounds"); return; }
                    ItemStack stack = parseStack(a[3]);
                    HolderLookup.Provider prov = getRegistryAccess();
                    t.items().set(idx, saveStack(prov, stack));
                    saveFile();
                    CommandManager.sendMessage("Set index " + idx + " in " + tab + ". Restart required.");
                } catch (Exception e) { CommandManager.sendMessage("Failed: " + e.getMessage()); }
            }
            case "icon" -> {
                if (a.length < 3) { CommandManager.sendMessage("Usage: .z creativetab modify <tab> icon <item>"); return; }
                ItemStack stack = parseStack(a[2]);
                HolderLookup.Provider prov = getRegistryAccess();
                Tab old = tabs.get(tab);
                tabs.put(tab, new Tab(saveStack(prov, stack), old.items()));
                try { saveFile(); CommandManager.sendMessage("Changed icon for " + tab + ". Restart required."); } catch (Exception e) { CommandManager.sendMessage("Failed: " + e.getMessage()); }
            }
            case "rename" -> {
                if (a.length < 3) { CommandManager.sendMessage("Usage: .z creativetab modify <tab> rename <newName>"); return; }
                String newName = a[2];
                if (Identifier.tryParse("zephyr:" + newName) == null) { CommandManager.sendMessage("Illegal name: " + newName); return; }
                Tab t = tabs.remove(tab);
                tabs.put(newName, t);
                try { saveFile(); CommandManager.sendMessage("Renamed " + tab + " to " + newName + ". Restart required."); } catch (Exception e) { CommandManager.sendMessage("Failed: " + e.getMessage()); }
            }
            default -> CommandManager.sendMessage("Unknown modify op: " + op);
        }
    }

    private HolderLookup.Provider getRegistryAccess() {
        var mc = Minecraft.getInstance();
        if (mc.level != null) return mc.level.registryAccess();
        if (mc.getConnection() != null) return mc.getConnection().registryAccess();
        return new net.minecraft.core.RegistryAccess.ImmutableRegistryAccess(BuiltInRegistries.REGISTRY.stream().toList());
    }

    private void addTab(String name, ItemStack icon) {
        if (tabs.containsKey(name)) { CommandManager.sendMessage("Tab already exists: " + name); return; }
        if (Identifier.tryParse("zephyr:" + name) == null) { CommandManager.sendMessage("Illegal characters in tab name"); return; }
        if (icon.isEmpty()) { CommandManager.sendMessage("Unknown icon item"); return; }
        try {
            var reg = getRegistryAccess();
            tabs.put(name, new Tab(saveStack(reg, icon), new ListTag()));
            saveFile();
            CommandManager.sendMessage("Added tab " + name + ". Restart required.");
        } catch (Exception e) { CommandManager.sendMessage("Failed: " + e.getMessage()); }
    }

    private void removeTab(String name) {
        if (!tabs.containsKey(name)) { CommandManager.sendMessage("Tab not found: " + name); return; }
        tabs.remove(name);
        try { saveFile(); CommandManager.sendMessage("Removed tab " + name + ". Restart required."); } catch (Exception e) { CommandManager.sendMessage("Failed: " + e.getMessage()); }
    }

    private ItemStack parseStack(String id) {
        try {
            Identifier ident = id.contains(":") ? Identifier.tryParse(id) : Identifier.tryParse("minecraft:" + id);
            if (ident == null) return ItemStack.EMPTY;
            var item = BuiltInRegistries.ITEM.getValue(ident);
            if (item == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
            return new ItemStack(item);
        } catch (Exception e) { return ItemStack.EMPTY; }
    }

    private CompoundTag saveStack(HolderLookup.Provider prov, ItemStack stack) {
        if (stack.isEmpty()) return new CompoundTag();
        try {
            Tag tag = ItemStack.CODEC.encodeStart(prov.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
            if (tag instanceof CompoundTag ct) return ct;
            CompoundTag wrapper = new CompoundTag();
            wrapper.put("tag", tag);
            return wrapper;
        } catch (Exception e) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            tag.putInt("count", stack.getCount());
            return tag;
        }
    }

    private static Optional<ItemStack> parseStackTag(HolderLookup.Provider prov, CompoundTag tag) {
        return ItemStack.CODEC.parse(prov.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial(err -> LOGGER.error("Invalid item: {}", err));
    }

    private static void saveFile() throws Exception {
        CompoundTag root = new CompoundTag();
        CompoundTag ct = new CompoundTag();
        for (var e : tabs.entrySet()) {
            CompoundTag tab = new CompoundTag();
            tab.put("icon", e.getValue().icon());
            tab.put("items", e.getValue().items());
            ct.put(e.getKey(), tab);
        }
        root.putInt("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
        root.put("CreativeTabs", ct);
        Files.createDirectories(CONFIG_DIR);
        Path newFile = File.createTempFile("creative_tabs", ".dat", CONFIG_DIR.toFile()).toPath();
        NbtIo.write(root, newFile);
        Path backup = CONFIG_DIR.resolve("creative_tabs.dat_old");
        Path cur = CONFIG_DIR.resolve("creative_tabs.dat");
        Util.safeReplaceFile(cur, newFile, backup);
    }

    private static void loadFile() throws Exception {
        tabs.clear();
        if (!Files.exists(CREATIVE_TABS_FILE)) return;
        CompoundTag root = NbtIo.read(CREATIVE_TABS_FILE);
        if (root == null) return;
        CompoundTag ct = root.getCompound("CreativeTabs").orElseGet(() -> root.getCompoundOrEmpty("Groups"));
        for (var entry : ct.entrySet()) {
            if (!(entry.getValue() instanceof CompoundTag tab)) continue;
            CompoundTag icon = tab.getCompoundOrEmpty("icon");
            ListTag items = tab.getListOrEmpty("items");
            tabs.put(entry.getKey(), new Tab(icon, items));
        }
    }

    private record Tab(CompoundTag icon, ListTag items) {
        void registerCreativeTab(HolderLookup.Provider builtin, String key) {
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("zephyr", key),
                    FabricCreativeModeTab.builder()
                            .title(Component.literal(key))
                            .icon(() -> parseStackTag(builtin, icon).orElse(ItemStack.EMPTY))
                            .displayItems((ctx, entries) -> {
                                for (int i = 0; i < items.size(); i++) {
                                    CompoundTag tag = items.getCompoundOrEmpty(i);
                                    parseStackTag(ctx.holders(), tag).ifPresent(entries::accept);
                                }
                            }).build());
        }
    }
}
