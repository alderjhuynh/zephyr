package com.zephyr.client.module.qol.freecam;

/**
 * The perspective the freecam camera starts in when the module is enabled:
 * {@link #INSIDE} keeps the camera at the player's eyes, {@link #FIRST_PERSON}
 * nudges it slightly forward, and the two third-person variants pull it back
 * (with {@link #THIRD_PERSON_MIRROR} additionally inverting the rotation).
 */
public enum Perspective {
    FIRST_PERSON,
    THIRD_PERSON,
    THIRD_PERSON_MIRROR,
    INSIDE,
}
