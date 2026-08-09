package com.zephyr.client.configplusgui.hud;

import com.zephyr.client.configplusgui.config.GlobalConfig;
import com.zephyr.client.configplusgui.config.ProfileManager;
import com.zephyr.client.configplusgui.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class PlaceholderEngine {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private PlaceholderEngine() {
    }

    public static String replace(String template) {
        if (template == null || template.isEmpty()) return template;

        String result = template;
        result = result.replace("%version%", version());
        result = result.replace("%profile%", ProfileManager.getActiveProfile());
        result = result.replace("%server%", server());
        result = result.replace("%player%", player());
        result = result.replace("%fps%", String.valueOf(Minecraft.getInstance().getFps()));
        result = result.replace("%modules_on%", String.valueOf(ModuleManager.enabledCount()));
        result = result.replace("%modules%", String.valueOf(ModuleManager.getModules().size()));
        result = result.replace("%theme%", theme());
        result = result.replace("%date%", LocalDate.now().format(DATE_FORMAT));
        result = result.replace("%time%", LocalTime.now().format(TIME_FORMAT));
        return result;
    }

    private static String version() {
        try {
            return FabricLoader.getInstance().getModContainer("zephyr")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("1.0.0");
        } catch (RuntimeException e) {
            return "1.0.0";
        }
    }

    private static String server() {
        Minecraft client = Minecraft.getInstance();
        if (client.getCurrentServer() != null) {
            return client.getCurrentServer().ip;
        }
        return client.getSingleplayerServer() != null ? "Singleplayer" : "";
    }

    private static String player() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null ? client.player.getName().getString() : "";
    }

    private static String theme() {
        return GlobalConfig.useCustomColor() ? "Custom" : GlobalConfig.themeColor().displayName();
    }
}
