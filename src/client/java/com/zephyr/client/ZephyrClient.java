package com.zephyr.client;

import com.zephyr.client.keybind.ZephyrKeybindManager;
import com.zephyr.client.module.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ZephyrClient implements ClientModInitializer {
	private final Sprint sprint = new Sprint();

	@Override
	public void onInitializeClient() {
		ZephyrConfig.load();

		ClientTickEvents.END_CLIENT_TICK.register(ZephyrKeybindManager::tick);

		//client tick events that don't have to do with buttons
		ClientTickEvents.END_CLIENT_TICK.register(Blink::tick);
		ClientTickEvents.END_CLIENT_TICK.register(ElytraBoost::tick);
		ClientTickEvents.END_CLIENT_TICK.register(PearlBoost::tick);
		ClientTickEvents.END_CLIENT_TICK.register(AntiHunger::onTick);
		ClientTickEvents.END_CLIENT_TICK.register(AirJump::tick);
		ClientTickEvents.END_CLIENT_TICK.register(LongJump::tick);
		ClientTickEvents.END_CLIENT_TICK.register(Aerodynamics::tick);
		ClientTickEvents.END_CLIENT_TICK.register(ElytraSwap::onTick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {sprint.onTick();});
		ClientTickEvents.END_CLIENT_TICK.register(client -> Sneak.onTick());
		ClientTickEvents.END_CLIENT_TICK.register(client -> HoldAttack.onTick());
		ClientTickEvents.END_CLIENT_TICK.register(client -> HoldUse.onTick());
		ClientTickEvents.END_CLIENT_TICK.register(client -> Step.tick());
		ClientTickEvents.END_CLIENT_TICK.register(client -> Jesus.tick());
		ClientTickEvents.END_CLIENT_TICK.register(client -> Flight.tick());
		ClientTickEvents.END_CLIENT_TICK.register(client -> SpeedMine.tick());
		ClientTickEvents.END_CLIENT_TICK.register(FreeCam::tick);
		ClientTickEvents.END_CLIENT_TICK.register(ItemRestock::tick);
		ClientTickEvents.END_CLIENT_TICK.register(HotbarRowSwap::tick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {DurabilitySwap.onTick();});
		ClientTickEvents.END_CLIENT_TICK.register(FastAttack::tick);
		ClientTickEvents.END_CLIENT_TICK.register(PeriodicUse::tick);
		ClientTickEvents.END_CLIENT_TICK.register(PeriodicAttack::tick);
		ClientTickEvents.END_CLIENT_TICK.register(FastUse::tick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> EntityControl.tick());
	}
}
