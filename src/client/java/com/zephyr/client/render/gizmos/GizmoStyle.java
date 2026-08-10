package com.zephyr.client.render.gizmos;

public final class GizmoStyle {
    private final int color;

    private GizmoStyle(int color) {
        this.color = color;
    }

    public static GizmoStyle stroke(int argb) {
        return new GizmoStyle(argb);
    }

    public int color() {
        return this.color;
    }
}
