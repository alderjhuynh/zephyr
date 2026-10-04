package com.zephyr.client.module.qol.auramap.waypoint;

import java.util.UUID;

public final class Waypoint {
    public enum Visibility { LOCAL, GLOBAL }
    public enum Kind { NORMAL, DEATH, DESTINATION }

    private final String id;
    private String name;
    private String initials;
    private int x;
    private int y;
    private int z;
    private boolean yIncluded = true;
    private WaypointColor color = WaypointColor.YELLOW;
    private Visibility visibility = Visibility.LOCAL;
    private boolean disabled;
    private boolean temporary;
    private Kind kind = Kind.NORMAL;
    private boolean rotation;
    private int yaw;
    private final long createdAt;

    public Waypoint(String name, String initials, int x, int y, int z, WaypointColor color) {
        this.id = UUID.randomUUID().toString();
        this.name = name == null || name.isBlank() ? "Waypoint" : name;
        this.initials = initials == null ? "" : initials;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color == null ? WaypointColor.YELLOW : color;
        this.createdAt = System.currentTimeMillis();
    }

    public Waypoint copy() {
        Waypoint c = new Waypoint(name, initials, x, y, z, color);
        c.yIncluded = yIncluded;
        c.visibility = visibility;
        c.disabled = disabled;
        c.temporary = temporary;
        c.kind = kind;
        c.rotation = rotation;
        c.yaw = yaw;
        return c;
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String n) { this.name = n == null || n.isBlank() ? "Waypoint" : n; }
    public String initials() { return initials; }
    public void setInitials(String s) { this.initials = s == null ? "" : s; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public void setPos(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    public boolean yIncluded() { return yIncluded; }
    public void setYIncluded(boolean v) { this.yIncluded = v; }
    public WaypointColor color() { return color; }
    public void setColor(WaypointColor c) { this.color = c == null ? WaypointColor.YELLOW : c; }
    public Visibility visibility() { return visibility; }
    public void setVisibility(Visibility v) { this.visibility = v == null ? Visibility.LOCAL : v; }
    public boolean disabled() { return disabled; }
    public void setDisabled(boolean v) { this.disabled = v; if (v) this.temporary = false; }
    public boolean temporary() { return temporary; }
    public void setTemporary(boolean v) { this.temporary = v; if (v) this.disabled = false; }
    public Kind kind() { return kind; }
    public void setKind(Kind k) { this.kind = k == null ? Kind.NORMAL : k; }
    public long createdAt() { return createdAt; }

    public boolean isDeath() { return kind == Kind.DEATH; }
    public boolean isDestination() { return kind == Kind.DESTINATION; }
    public boolean isGlobal() { return visibility == Visibility.GLOBAL || isDeath(); }
    public boolean rotation() { return rotation; }
    public void setRotation(boolean v) { this.rotation = v; }
    public int yaw() { return yaw; }
    public void setYaw(int v) { this.yaw = v; }

    public String comparisonName() {
        String s = name().toLowerCase().trim();
        if (s.startsWith("the ")) return s.substring(4);
        if (s.startsWith("a ")) return s.substring(2);
        return s;
    }

    public double distSq(double px, double py, double pz) {
        double dx = x - px;
        double dy = yIncluded ? (y - py) : 0.0;
        double dz = z - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    public double dist2D(double px, double pz) {
        double dx = x - px;
        double dz = z - pz;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
