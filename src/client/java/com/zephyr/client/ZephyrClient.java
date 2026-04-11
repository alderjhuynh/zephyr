package com.zephyr.client;

import com.zephyr.client.gui.ZephyrMenuScreen;
import com.zephyr.client.module.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ZephyrClient implements ClientModInitializer {
	private static final int ZEPHYR_MENU_MODIFIER_KEY = GLFW.GLFW_KEY_L;
	private static final int ZEPHYR_MENU_TRIGGER_KEY = GLFW.GLFW_KEY_ENTER;

	private final Sprint sprint = new Sprint();
	private boolean zephyrMenuChordHeld;

	@Override
	public void onInitializeClient() {
		ZephyrConfig.load();

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

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			long windowHandle = client.getWindow().getHandle();
			boolean chordPressed = InputUtil.isKeyPressed(windowHandle, ZEPHYR_MENU_MODIFIER_KEY)
					&& InputUtil.isKeyPressed(windowHandle, ZEPHYR_MENU_TRIGGER_KEY);

			if (chordPressed && !zephyrMenuChordHeld) {
				Screen currentScreen = client.currentScreen;
				if (!(currentScreen instanceof ZephyrMenuScreen)) {
					client.setScreen(new ZephyrMenuScreen(currentScreen));
				}
			}

			zephyrMenuChordHeld = chordPressed;
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
