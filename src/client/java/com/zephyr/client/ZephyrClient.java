package com.zephyr.client;

import com.zephyr.client.TickScheduler;
import com.zephyr.client.configplusgui.secretsettings.bettermovement.BetterMovement;
import com.zephyr.client.configplusgui.config.GlobalConfig;
import com.zephyr.client.configplusgui.config.ProfileManager;
import com.zephyr.client.configplusgui.hud.HudRenderer;
import com.zephyr.client.configplusgui.hud.NotificationManager;
import com.zephyr.client.configplusgui.hud.PartyManager;
import com.zephyr.client.configplusgui.keybind.GuiKeybindHandler;
import com.zephyr.client.configplusgui.keybind.KeybindManager;
import com.zephyr.client.configplusgui.module.ModuleManager;
import com.zephyr.client.commands.CommandManager;
import com.zephyr.client.commands.CommandPrefixHandler;
import com.zephyr.client.commands.ZCommand;
import com.zephyr.client.discord.DiscordPresenceManager;
import com.zephyr.client.module.bots.pathing.TargetRender;
import com.zephyr.client.module.qol.jade.JadeRenderer;
import com.zephyr.client.module.qol.shulkerboxtooltip.ShulkerBoxTooltipProviders;
import com.zephyr.client.module.qol.shulkerboxtooltip.tooltip.PreviewClientTooltipComponent;
import com.zephyr.client.module.qol.shulkerboxtooltip.tooltip.PreviewTooltipComponent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Zephyr's client-side entry point (Fabric {@link ClientModInitializer}).
 * Wires up the whole client: initializes configuration, modules, profiles and
 * keybinds, registers the {@code .z} command, hooks Discord Rich Presence to
 * client lifecycle and play connection events, registers tooltip providers, and
 * attaches per-tick handling plus HUD renderers (notifications and the HUD
 * overlay). Modules are periodically autosaved when the configured interval
 * elapses, and always saved on client stop.
 */
public class
ZephyrClient implements ClientModInitializer {
	private final GuiKeybindHandler guiKeybindHandler = new GuiKeybindHandler();

	private long tickCount = 0;

	/**
	 * Initializes every client-side subsystem and registers Fabric event
	 * handlers for lifecycle, tick, play connection, tooltip and HUD events.
	 */
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
		CommandManager.register(ZCommand.INSTANCE);

		ShulkerBoxTooltipProviders.register();

		// PreviewTooltipComponent -> PreviewClientTooltipComponent conversion for ShulkerBoxTooltip.
		ClientTooltipComponentCallback.EVENT.register(data -> {
			if (data instanceof PreviewTooltipComponent previewData)
				return new PreviewClientTooltipComponent(previewData);
			return null;
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			TickScheduler.tick();
			guiKeybindHandler.tick(client);
			CommandPrefixHandler.tick(client);
			ModuleManager.tick(client);
			PartyManager.tick();
			BetterMovement.tick(client);

			tickCount++;
			double interval = GlobalConfig.autosaveIntervalSeconds();
			if (interval > 0 && tickCount % Math.max(1, (long) (interval * 20)) == 0) {
				ModuleManager.saveAll();
			}
		});

		HudElementRegistry.attachElementBefore(net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath("zephyr", "notifications"),
				(graphics, tickCounter) -> {
					Minecraft client = Minecraft.getInstance();
					NotificationManager.render(graphics, client.font,
							client.getWindow().getGuiScaledWidth(),
							client.getWindow().getGuiScaledHeight());
				});

		HudElementRegistry.attachElementBefore(net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath("zephyr", "hud_overlay"),
				(graphics, tickCounter) -> {
					Minecraft client = Minecraft.getInstance();
					HudRenderer.render(graphics, client.font,
							client.getWindow().getGuiScaledWidth(),
							client.getWindow().getGuiScaledHeight());
					JadeRenderer.render(graphics, tickCounter.getRealtimeDeltaTicks());
				});

		TargetRender.init();


		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleManager.saveAll());
	}
}