package com.zephyr.client.cornerstone;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import com.zephyr.client.commands.CommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Sends queued {@code setblock}/{@code fill} commands at
 * {@link CornerstoneStore#commandsPerTick()} per client tick, shows progress
 * above the hotbar, and hides vanilla command feedback while running.
 */
public final class CornerstoneRunner {
    private static final Deque<String> QUEUE = new ArrayDeque<>();
    private static int total;
    private static int quietTicks;

    private CornerstoneRunner() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(CornerstoneRunner::tick);

        ClientReceiveMessageEvents.ALLOW_GAME.register(
                (message, overlay) -> overlay || !isBusyOrQuiet() || !isBlockCommandFeedback(message));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> cancel());
    }

    public static void start(List<String> commands) {
        QUEUE.clear();
        QUEUE.addAll(commands);
        total = commands.size();
    }

    public static int cancel() {
        int dropped = QUEUE.size();
        QUEUE.clear();
        return dropped;
    }

    public static boolean isBusy() {
        return !QUEUE.isEmpty();
    }

    private static boolean isBusyOrQuiet() {
        return !QUEUE.isEmpty() || quietTicks > 0;
    }

    /** Called every client tick; also callable from Zephyr's tick handler if needed. */
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            QUEUE.clear();
            return;
        }

        if (QUEUE.isEmpty()) {
            if (quietTicks > 0) quietTicks--;
            return;
        }

        int perTick = CornerstoneStore.commandsPerTick();
        for (int i = 0; i < perTick && !QUEUE.isEmpty(); i++) {
            player.connection.sendCommand(QUEUE.poll());
        }
        quietTicks = 40;

        if (QUEUE.isEmpty()) {
            player.sendSystemMessage(Component.literal("Cornerstone: finished (" + total + " commands)."));
        } else if (player.tickCount % 10 == 0) {
            int done = total - QUEUE.size();
            player.sendOverlayMessage(
                    Component.literal("Cornerstone: " + (done * 100 / total) + "% (" + done + "/" + total + ")"));
        }
    }

    /** Client-side helper so the command class can report without touching chat directly. */
    static void tell(String message) {
        CommandManager.sendMessage(message);
    }

    private static boolean isBlockCommandFeedback(Component message) {
        if (message.getContents() instanceof TranslatableContents tc) {
            String key = tc.getKey();
            if (key.startsWith("commands.setblock.") || key.startsWith("commands.fill.")) {
                return true;
            }
        }
        for (Component sibling : message.getSiblings()) {
            if (isBlockCommandFeedback(sibling)) return true;
        }
        return false;
    }
}
