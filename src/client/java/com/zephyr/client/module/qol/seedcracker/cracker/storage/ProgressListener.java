package com.zephyr.client.module.qol.seedcracker.cracker.storage;

import com.zephyr.client.module.qol.seedcracker.util.Log;

/**
 * Tracks search progress for the time machine's structure seed phase.
 *
 * <p>Accumulates percent-complete contributions and, in debug mode, logs progress every fourth
 * update to avoid spamming the chat.
 */
public class ProgressListener {

    /** Current accumulated progress, in percent. */
    protected float progress;
    /** Number of updates received so far. */
    protected int count = 0;

    public ProgressListener() {
        this(0.0F);
    }

    public ProgressListener(float progress) {
        this.progress = progress;
    }

    /**
     * Adds a percent-complete increment, logging progress every fourth call in debug mode.
     *
     * @param percent the progress increment
     * @param debug whether to log progress updates
     */
    public synchronized void addPercent(float percent, boolean debug) {
        if ((this.count & 3) == 0 && debug) {
            Log.debug(Log.translate("tmachine.progress") + ": " + this.progress + "%");
        }

        this.count++;
        this.progress += percent;
    }

}
