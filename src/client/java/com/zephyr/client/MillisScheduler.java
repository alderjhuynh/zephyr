package com.zephyr.client;

import net.minecraft.client.Minecraft;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MillisScheduler {

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "MillisScheduler");
        thread.setDaemon(true);
        return thread;
    });

    private MillisScheduler() {
    }

    public static void schedule(long delayMillis, Runnable action) {
        EXECUTOR.schedule(() -> {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                client.execute(action);
            }
        }, delayMillis, TimeUnit.MILLISECONDS);
    }
}
