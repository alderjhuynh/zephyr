package com.zephyr.client.commands;

import com.zephyr.client.configplusgui.config.ProfileManager;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The root {@code .z} command. With no arguments it prints client diagnostics
 * (version, active profile, enabled module count, command prefix); it also
 * delegates to the {@code module} subcommand (enabling/disabling/toggling any
 * registered module by name) and to {@link SeedcrackerCommand} for the
 * {@code seedcracker} subcommand.
 */
public final class ZCommand extends Command {
    public static final ZCommand INSTANCE = new ZCommand();

    private ZCommand() {
        super("z", "Shows diagnostics or controls modules: .z module <name> <on|off|toggle> | .z seedcracker <command>");
    }

    /**
     * Offers tab-completion for the {@code module} subcommand (module names and
     * state keywords) and delegates to the seedcracker command's own suggestions
     * for the {@code seedcracker} subcommand.
     *
     * @param args the arguments parsed up to the cursor
     * @return the candidate completions for the current argument position
     */
    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            return List.of("module", "seedcracker");
        }
        if (args[0].equalsIgnoreCase("module")) {
            if (args.length == 2) {
                return ModuleManager.getModules().stream().map(Module::getName).toList();
            }
            if (args.length >= 3) {
                return List.of("on", "off", "toggle");
            }
        }
        if (args[0].equalsIgnoreCase("seedcracker")) {
            return SeedcrackerCommand.INSTANCE.suggest(Arrays.copyOfRange(args, 1, args.length));
        }
        return List.of("module", "seedcracker");
    }

    /**
     * Dispatches the root command: prints diagnostics when invoked bare,
     * forwards the {@code module} subcommand to the module control handler, and
     * forwards {@code seedcracker} arguments to {@link SeedcrackerCommand}.
     *
     * @param args the positional arguments after {@code .z}
     */
    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            diagnostics();
            return;
        }
        if (args[0].equalsIgnoreCase("module")) {
            module(args);
            return;
        }
        if (args[0].equalsIgnoreCase("seedcracker")) {
            SeedcrackerCommand.INSTANCE.execute(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        CommandManager.sendMessage("Usage: .z [module <name> <on|off|toggle> | seedcracker <command>]");
    }

    private void diagnostics() {
        String prefix = CommandPrefixHandler.currentPrefix();
        String prefixText = prefix == null ? "None" : prefix;

        CommandManager.sendMessage("Zephyr v" + version()
                + " | Profile: " + ProfileManager.getActiveProfile());
        CommandManager.sendMessage("Modules: " + ModuleManager.enabledCount() + "/"
                + ModuleManager.getModules().size() + " enabled | Prefix: " + prefixText);
    }

    private void module(String[] args) {
        if (args.length < 3) {
            CommandManager.sendMessage("Usage: .z module <name> <on|off|toggle>");
            return;
        }

        String state = args[args.length - 1];
        if (!isState(state)) {
            CommandManager.sendMessage("Usage: .z module <name> <on|off|toggle>");
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));

        Module module = ModuleManager.get(name);
        if (module == null) {
            CommandManager.sendMessage("Unknown module: " + name);
            return;
        }

        switch (state.toLowerCase(Locale.ROOT)) {
            case "on" -> module.setEnabled(true);
            case "off" -> module.setEnabled(false);
            case "toggle" -> module.toggle();
        }
        CommandManager.sendMessage(module.getName() + " is now " + (module.isEnabled() ? "ON" : "OFF"));
    }

    private static boolean isState(String token) {
        return token.equalsIgnoreCase("on") || token.equalsIgnoreCase("off") || token.equalsIgnoreCase("toggle");
    }

    private static String version() {
        return FabricLoader.getInstance()
                .getModContainer("zephyr")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
