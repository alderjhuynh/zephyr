package com.zephyr.client.module.qol.freecam;

/**
 * How the freecam camera moves. {@link #DEFAULT} uses the vanilla horizontal
 * flight vector with a configurable vertical speed; {@link #CREATIVE} mimics
 * creative-mode flying, boosting vertical movement through jump and sneak keys.
 */
public enum FlightMode {
    CREATIVE, DEFAULT
}
