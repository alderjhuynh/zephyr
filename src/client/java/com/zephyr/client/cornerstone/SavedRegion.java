package com.zephyr.client.cornerstone;

import java.util.ArrayList;
import java.util.List;

/**
 * A saved region: origin (lowest corner where it was captured), size, and the
 * {@code setblock}/{@code fill} commands with origin-relative ({@code ~}) coordinates.
 */
public final class SavedRegion {
    public int originX, originY, originZ;
    public int sizeX, sizeY, sizeZ;
    public List<String> commands = new ArrayList<>();
}
