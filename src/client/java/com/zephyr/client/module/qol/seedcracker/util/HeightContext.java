package com.zephyr.client.module.qol.seedcracker.util;

/**
 * Describes the world's vertical generation range.
 *
 * <p>Captures the bottom and top Y of the current dimension so finders and decorators can compute
 * heights and height-relative random bounds consistently.
 */
public class HeightContext {
    private final int bottomY;
    private final int topY;

    public HeightContext(int minY, int maxY) {
        this.bottomY = minY;
        this.topY = maxY;
    }

    /**
     * @return the top Y of the generation range
     */
    public int getTopY() {
        return topY;
    }

    /**
     * @return the bottom Y of the generation range
     */
    public int getBottomY() {
        return bottomY;
    }

    /**
     * @return the total vertical size of the generation range
     */
    public int getHeight() {
        return topY - bottomY;
    }

    /**
     * @param yValue a world Y
     * @return the distance of the given Y from the bottom of the range
     */
    public int getDistanceToBottom(int yValue) {
        return yValue - bottomY;
    }

    /**
     * @param yValue a world Y
     * @return the distance of the given Y from the top of the range
     */
    public int getDistanceToTop(int yValue) {
        return topY - yValue - 1;
    }
}
