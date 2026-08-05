package com.zephyr.client;

import com.zephyr.client.configplusgui.*;
import com.zephyr.client.discord.DiscordPresenceManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class
ZephyrClient implements ClientModInitializer {
	private final GuiKeybindHandler guiKeybindHandler = new GuiKeybindHandler();

	@Override
	public void onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> DiscordPresenceManager.initialize());
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DiscordPresenceManager.shutdown());
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> DiscordPresenceManager.onWorldTransition());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DiscordPresenceManager.onWorldTransition());


		GlobalConfig.init();
		ModuleManager.init();
		ProfileManager.init();
		KeybindManager.init();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			guiKeybindHandler.tick(client);
			ModuleManager.tick(client);
		});

		HudElementRegistry.attachElementBefore(net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath("zephyr", "notifications"),
				(graphics, tickCounter) -> {
					Minecraft client = Minecraft.getInstance();
					NotificationManager.render(graphics, client.font,
							client.getWindow().getGuiScaledWidth(),
							client.getWindow().getGuiScaledHeight());
				});


		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleManager.saveAll());
	}
}