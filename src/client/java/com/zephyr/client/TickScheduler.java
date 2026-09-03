package com.zephyr.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TickScheduler {

    private static class ScheduledTask {
        int ticksRemaining;
        Runnable action;

        ScheduledTask(int delay, Runnable action) {
            this.ticksRemaining = delay;
            this.action = action;
        }
    }

    private static final List<ScheduledTask> tasks = new ArrayList<>();

    public static void schedule(int delay, Runnable action) {
        tasks.add(new ScheduledTask(delay, action));
    }

    public static void tick() {
        List<Runnable> ready = new ArrayList<>();

        Iterator<ScheduledTask> it = tasks.iterator();

        while (it.hasNext()) {
            ScheduledTask task = it.next();

            task.ticksRemaining--;

            if (task.ticksRemaining <= 0) {
                ready.add(task.action);
                it.remove();
            }
        }

        for (Runnable action : ready) {
            action.run();
        }
    }
}
