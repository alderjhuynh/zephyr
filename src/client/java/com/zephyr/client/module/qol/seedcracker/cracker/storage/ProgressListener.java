package com.zephyr.client.module.qol.seedcracker.cracker.storage;

import com.zephyr.client.module.qol.seedcracker.util.Log;

public class ProgressListener {

    protected float progress;
    protected int count = 0;

    public ProgressListener() {
        this(0.0F);
    }

    public ProgressListener(float progress) {
        this.progress = progress;
    }

    public synchronized void addPercent(float percent, boolean debug) {
        if ((this.count & 3) == 0 && debug) {
            Log.debug(Log.translate("tmachine.progress") + ": " + this.progress + "%");
        }

        this.count++;
        this.progress += percent;
    }

}
