package com.zephyr.client.discord;

import com.google.gson.JsonObject;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.User;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import com.zephyr.client.module.qol.DiscordPresence;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public final class DiscordPresenceManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("discord-rpc-test");

	private static final long CLIENT_ID = 1533729640638840952L;

	private static final long RECONNECT_DELAY_SECONDS = 15L;

	private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
		Thread thread = new Thread(runnable, "discord-rpc-test-ipc");
		thread.setDaemon(true);
		return thread;
	});

	private static volatile IPCClient ipcClient;
	private static volatile boolean ready = false;
	private static volatile boolean shuttingDown = false;
	private static volatile long sessionStart = System.currentTimeMillis();

	private DiscordPresenceManager() {
	}

	public static void initialize() {
		sessionStart = System.currentTimeMillis();
		if (DiscordPresence.INSTANCE.isEnabled()) {
			enable();
		}
	}

	public static void enable() {
		if (shuttingDown) {
			return;
		}
		EXECUTOR.execute(DiscordPresenceManager::connect);
	}

	public static void disable() {
		EXECUTOR.execute(DiscordPresenceManager::disconnect);
	}

	public static void onWorldTransition() {
		sessionStart = System.currentTimeMillis();
		pushPresence();
	}

	public static void refresh() {
		pushPresence();
	}

	public static void shutdown() {
		shuttingDown = true;
		EXECUTOR.execute(DiscordPresenceManager::disconnect);
		EXECUTOR.shutdown();
	}

	private static void connect() {
		if (shuttingDown) {
			return;
		}

		IPCClient client = new IPCClient(CLIENT_ID);
		client.setListener(new IPCListener() {
			@Override
			public void onReady(IPCClient readyClient) {
				LOGGER.info("Connected to Discord for Rich Presence.");
				ready = true;
				pushPresence();
			}

			@Override
			public void onClose(IPCClient closedClient, JsonObject json) {
				ready = false;
				ipcClient = null;
				if (!shuttingDown) {
					LOGGER.debug("Discord IPC connection closed.");
					scheduleReconnectIfStillEnabled();
				}
			}

			@Override
			public void onDisconnect(IPCClient disconnectedClient, Throwable t) {
				ready = false;
				ipcClient = null;
				if (!shuttingDown) {
					LOGGER.debug("Discord IPC connection was lost.", t);
					scheduleReconnectIfStillEnabled();
				}
			}

			@Override
			public void onPacketSent(IPCClient sendingClient, Packet packet) {
			}

			@Override
			public void onPacketReceived(IPCClient receivingClient, Packet packet) {
			}

			@Override
			public void onActivityJoin(IPCClient joiningClient, String secret) {
			}

			@Override
			public void onActivitySpectate(IPCClient spectatingClient, String secret) {
			}

			@Override
			public void onActivityJoinRequest(IPCClient requestingClient, String secret, User user) {
			}
		});

		try {
			client.connect();
			ipcClient = client;
		} catch (NoDiscordClientException e) {
			scheduleReconnectIfStillEnabled();
		} catch (Exception e) {
			LOGGER.debug("Could not connect to Discord for Rich Presence.", e);
			scheduleReconnectIfStillEnabled();
		}
	}

	private static void scheduleReconnectIfStillEnabled() {
		if (shuttingDown) {
			return;
		}
		EXECUTOR.schedule(() -> {
			if (!shuttingDown && DiscordPresence.INSTANCE.isEnabled() && ipcClient == null) {
				connect();
			}
		}, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
	}

	private static void disconnect() {
		IPCClient client = ipcClient;
		ipcClient = null;
		ready = false;
		if (client != null) {
			try {
				client.close();
			} catch (Exception e) {
				LOGGER.debug("Failed to cleanly close the Discord IPC connection.", e);
			}
		}
	}

	private static void pushPresence() {
		if (!ready || !DiscordPresence.INSTANCE.isEnabled()) {
			return;
		}
		EXECUTOR.execute(() -> {
			IPCClient client = ipcClient;
			if (client == null || !ready) {
				return;
			}
			try {
				client.sendRichPresence(buildPresence());
			} catch (Exception e) {
				LOGGER.debug("Failed to update Discord Rich Presence.", e);
			}
		});
	}

	private static RichPresence buildPresence() {
		Minecraft client = Minecraft.getInstance();
		String version = SharedConstants.getCurrentVersion().name();
		String details = "Playing Minecraft " + version;
		String state = describeLocation(client);

		return new RichPresence.Builder()
				.setDetails(details)
				.setState(state)
				.setStartTimestamp(sessionStart)
				.build();
	}

	private static String describeLocation(Minecraft client) {
		if (client.level == null) {
			return "In Main Menu";
		}

		if (client.isLocalServer()) {
			IntegratedServer singleplayerServer = client.getSingleplayerServer();
			if (singleplayerServer != null) {
				return "In World " + singleplayerServer.getWorldData().getLevelName();
			}
			return "In a World";
		}

		ServerData server = client.getCurrentServer();
		if (server != null) {
			String label = server.isRealm() ? "Realm" : "Server";
			return "In " + label + " " + server.name;
		}

		return "In a World";
	}
}