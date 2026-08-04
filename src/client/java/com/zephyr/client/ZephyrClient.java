package com.zephyr.client;

import com.zephyr.client.configplusgui.KeybindManager;
import com.zephyr.client.configplusgui.ModuleManager;
import com.zephyr.client.configplusgui.GuiKeybindHandler;
import com.zephyr.client.configplusgui.ProfileManager;
import com.zephyr.client.discord.DiscordPresenceManager;
import com.zephyr.client.module.combat.KillWyvern;
import com.zephyr.client.module.movement.AirJump;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ZephyrClient implements ClientModInitializer {
	private final GuiKeybindHandler guiKeybindHandler = new GuiKeybindHandler();

	@Override
	public void onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> DiscordPresenceManager.initialize());
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DiscordPresenceManager.shutdown());
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> DiscordPresenceManager.onWorldTransition());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DiscordPresenceManager.onWorldTransition());


		ModuleManager.init();
		ProfileManager.init();
		KeybindManager.init();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			guiKeybindHandler.tick(client);
			ModuleManager.tick(client);
		});


		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleManager.saveAll());
	}
}
