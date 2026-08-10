package com.zephyr.client.module.qol.freecam;

/**
 * Where interactions (attacking/using) originate while freecam is active:
 * {@link #CAMERA} performs them from the detached camera's position, while
 * {@link #PLAYER} performs them from the frozen player's position.
 */
public enum InteractionMode {
    CAMERA, PLAYER
}
