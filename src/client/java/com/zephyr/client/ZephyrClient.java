package com.zephyr.client;

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
import com.zephyr.client.commands.FakePlayerCommand;
import com.zephyr.client.commands.ZCommand;
import com.zephyr.client.TickScheduler;
import com.zephyr.client.cornerstone.CornerstoneRunner;
import com.zephyr.client.cornerstone.CornerstoneSaver;
import com.zephyr.client.cornerstone.CornerstoneSelection;
import com.zephyr.client.cornerstone.CornerstoneSelectionRenderer;
import com.zephyr.client.cornerstone.CornerstoneStore;
import com.zephyr.client.fakeplayer.FakePlayerManager;
import com.zephyr.client.module.bots.pathing.TargetRender;
import com.zephyr.client.module.combat.TotemPopNotifier;
import com.zephyr.client.discord.DiscordPresenceManager;
import com.zephyr.client.module.qol.jade.JadeRenderer;
import com.zephyr.client.module.qol.shulkerboxtooltip.ShulkerBoxTooltipProviders;
import com.zephyr.client.module.qol.shulkerboxtooltip.tooltip.PreviewClientTooltipComponent;
import com.zephyr.client.module.qol.shulkerboxtooltip.tooltip.PreviewTooltipComponent;
import com.zephyr.client.render.gizmos.Gizmos;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.Minecraft;

public class
ZephyrClient implements ClientModInitializer {
	private final GuiKeybindHandler guiKeybindHandler = new GuiKeybindHandler();

	private long tickCount = 0;

	@Override
	public void onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> DiscordPresenceManager.initialize());
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DiscordPresenceManager.shutdown());
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> DiscordPresenceManager.onWorldTransition());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			DiscordPresenceManager.onWorldTransition();
			FakePlayerManager.onDisconnect();
		});


		GlobalConfig.init();
		ModuleManager.init();
		ProfileManager.init();
		KeybindManager.init();
		CommandManager.register(ZCommand.INSTANCE);
		CommandManager.register(FakePlayerCommand.INSTANCE);

		CornerstoneStore.load();
		CornerstoneSelection.register();
		CornerstoneSelectionRenderer.register();
		CornerstoneRunner.register();
		CornerstoneSaver.register();

		TargetRender.init();
		JadeRenderer.init();

		HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
			Minecraft client = Minecraft.getInstance();
			TotemPopNotifier.INSTANCE.render(graphics, client.font,
					client.getWindow().getGuiScaledWidth());
		});

		ShulkerBoxTooltipProviders.register();

		// MouseTweaks wheel tweak: AbstractContainerScreen does not override
		// mouseScrolled in 1.21.1, so it cannot be mixed in directly.
		// Handle scroll via Fabric's ScreenMouseEvents instead.
		ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			ScreenMouseEvents.allowMouseScroll(screen).register((scr, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
				if (!(scr instanceof AbstractContainerScreen<?> containerScreen)) return true;
				double delta = verticalAmount != 0 ? verticalAmount : horizontalAmount;
				boolean handled = com.zephyr.client.module.qol.mousetweaks.MouseTweaksHandler.onMouseScrolled(
						containerScreen, mouseX, mouseY, delta);
				// allowMouseScroll: false blocks vanilla handling.
				return !handled;
			});
		});

		// PreviewTooltipComponent -> PreviewClientTooltipComponent conversion for ShulkerBoxTooltip.
		TooltipComponentCallback.EVENT.register(data -> {
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

		HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
			Minecraft client = Minecraft.getInstance();
			NotificationManager.render(graphics, client.font,
					client.getWindow().getGuiScaledWidth(),
					client.getWindow().getGuiScaledHeight());
		});

		HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
			Minecraft client = Minecraft.getInstance();
			HudRenderer.render(graphics, client.font,
					client.getWindow().getGuiScaledWidth(),
					client.getWindow().getGuiScaledHeight());
		});

		WorldRenderEvents.END.register(context ->
				Gizmos.render(context.camera(), context.positionMatrix()));

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleManager.saveAll());
	}
}