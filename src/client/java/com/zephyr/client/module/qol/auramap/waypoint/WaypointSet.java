package com.zephyr.client.module.qol.auramap.waypoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WaypointSet {
    private String name;
    private final List<Waypoint> waypoints = new ArrayList<>();

    public WaypointSet(String name) {
        this.name = name == null || name.isBlank() ? "default" : name;
    }

    public String name() { return name; }
    public void rename(String n) { if (n != null && !n.isBlank()) this.name = n; }
    public List<Waypoint> all() { return Collections.unmodifiableList(waypoints); }
    List<Waypoint> mutable() { return waypoints; }

    public void add(Waypoint w) { if (w != null) waypoints.add(w); }
    public void addFirst(Waypoint w) { if (w != null) waypoints.add(0, w); }
    public boolean remove(Waypoint w) { return waypoints.remove(w); }
    public Waypoint remove(int i) { return waypoints.remove(i); }
    public int size() { return waypoints.size(); }
    public boolean isEmpty() { return waypoints.isEmpty(); }
    public Waypoint get(int i) { return waypoints.get(i); }
}
