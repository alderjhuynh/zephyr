package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.discord.DiscordPresenceManager;

public final class DiscordPresence extends Module {
    public static final DiscordPresence INSTANCE = new DiscordPresence();
    private DiscordPresence() {
        super("Discord Presence", "Shows Zephyr Client instead of Minecraft as your Discord Presence", Category.QOL, true);
    }

    @Override
    protected void onEnable() {
        DiscordPresenceManager.enable();
    }

    @Override
    protected void onDisable() {
        DiscordPresenceManager.disable();
    }
}
