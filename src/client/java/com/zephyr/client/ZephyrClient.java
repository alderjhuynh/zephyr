package com.zephyr.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import org.lwjgl.glfw.GLFW;

public class ZephyrClient implements ClientModInitializer {

	private final Sprint sprint = new Sprint();

	@Override
	public void onInitializeClient() {
		//keybind registering
		KeyBinding blinkKey = KeyBindingHelper.registerKeyBinding(
				new KeyBinding(
						"key.zephyr.blink",
						GLFW.GLFW_KEY_B,
						"category.zephyr"
				)
		);

		//button-y client tick events
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (blinkKey.wasPressed()) {
				if (!Blink.CanUseKeybind) {return;}
				Blink.toggle();
			}
		});

		//client tick events that don't have to do with buttons
		ClientTickEvents.END_CLIENT_TICK.register(Blink::tick);
		ClientTickEvents.END_CLIENT_TICK.register(ElytraBoost::tick);
		ClientTickEvents.END_CLIENT_TICK.register(PearlBoost::tick);
		ClientTickEvents.END_CLIENT_TICK.register(AntiHunger::onTick);
		ClientTickEvents.END_CLIENT_TICK.register(AirJump::tick);
		ClientTickEvents.END_CLIENT_TICK.register(LongJump::tick);
		ClientTickEvents.END_CLIENT_TICK.register(Aerodynamics::tick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {sprint.onTick();});
		ClientTickEvents.END_CLIENT_TICK.register(client -> Step.tick());
		ClientTickEvents.END_CLIENT_TICK.register(client -> Jesus.tick());
		ClientTickEvents.END_CLIENT_TICK.register(client -> Flight.tick());
		ClientTickEvents.END_CLIENT_TICK.register(client -> SpeedMine.tick());
		ClientTickEvents.END_CLIENT_TICK.register(ItemRestock::tick);
	}
}
