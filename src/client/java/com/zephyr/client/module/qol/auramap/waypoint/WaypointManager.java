package com.zephyr.client.module.qol.auramap.waypoint;

import com.zephyr.client.module.qol.auramap.AuraMapBridge;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WaypointManager {
    private static final WaypointManager INSTANCE = new WaypointManager();

    private Path dimRoot;
    private String worldLabel = "unknown";
    private final Map<String, WaypointSet> sets = new LinkedHashMap<>();
    private String currentSet = "default";
    private boolean wasAlive = true;
    private long lastSaveMs;
    private long setChangedMs;
    private WaypointSort sort = WaypointSort.NONE;

    private WaypointManager() {
        sets.put("default", new WaypointSet("default"));
    }

    public static WaypointManager get() { return INSTANCE; }

    public synchronized void setContext(Path dimRoot, String worldLabel) {
        String key = dimRoot == null ? null : dimRoot.toString();
        String cur = this.dimRoot == null ? null : this.dimRoot.toString();
        if (key != null && key.equals(cur)) {
            this.worldLabel = worldLabel;
            return;
        }
        saveNow();
        this.dimRoot = dimRoot;
        this.worldLabel = worldLabel;
        sets.clear();
        currentSet = "default";
        if (dimRoot != null) {
            var st = WaypointStore.load(dimRoot);
            sets.putAll(st.sets());
            currentSet = st.currentSet();
        }
        if (sets.isEmpty()) sets.put("default", new WaypointSet("default"));
        if (!sets.containsKey(currentSet)) currentSet = sets.keySet().iterator().next();
    }

    public synchronized void clearContext() {
        saveNow();
        dimRoot = null;
        sets.clear();
        sets.put("default", new WaypointSet("default"));
        currentSet = "default";
    }

    public synchronized WaypointSet currentSet() {
        return sets.computeIfAbsent(currentSet, WaypointSet::new);
    }

    public synchronized List<WaypointSet> sets() { return new ArrayList<>(sets.values()); }
    public synchronized String currentSetName() { return currentSet; }

    public synchronized void switchSet(String name) {
        if (name == null || name.isBlank()) return;
        sets.computeIfAbsent(name, WaypointSet::new);
        currentSet = name;
        setChangedMs = System.currentTimeMillis();
        saveSoon();
    }

    public synchronized long setChangedMs() { return setChangedMs; }
    public synchronized void clearSetChanged() { setChangedMs = 0; }
    public synchronized WaypointSort sort() { return sort; }
    public synchronized void setSort(WaypointSort s) { sort = s == null ? WaypointSort.NONE : s; }

    public synchronized void addSet(String name) {
        if (name == null || name.isBlank()) return;
        sets.computeIfAbsent(name, WaypointSet::new);
        saveSoon();
    }

    public synchronized void removeSet(String name) {
        if (name == null || sets.size() <= 1) return;
        sets.remove(name);
        if (currentSet.equals(name)) currentSet = sets.keySet().iterator().next();
        saveSoon();
    }

    public synchronized void add(Waypoint w) {
        add(w, false);
    }

    public synchronized void add(Waypoint w, boolean toTop) {
        if (w == null) return;
        if (toTop) currentSet().addFirst(w);
        else currentSet().add(w);
        saveSoon();
    }

    public synchronized void setTemporary(int x, int y, int z, boolean yIncluded) {
        WaypointSet set = currentSet();
        Waypoint old = null;
        for (Waypoint w : set.all()) {
            if (w.temporary() || w.isDestination()) { old = w; break; }
        }
        if (old != null) set.mutable().remove(old);
        Waypoint t = new Waypoint("Destination", "X", x, y, z, WaypointColor.random());
        t.setKind(Waypoint.Kind.DESTINATION);
        t.setTemporary(true);
        t.setYIncluded(yIncluded);
        set.addFirst(t);
        saveSoon();
    }

    public synchronized void clearTemporary() {
        for (WaypointSet s : sets.values()) {
            s.mutable().removeIf(w -> w.temporary() || w.isDestination());
        }
        saveSoon();
    }

    /** Removes every waypoint in the current set (the set itself is kept). */
    public synchronized void clearCurrentSet() {
        currentSet().mutable().clear();
        saveSoon();
    }

    public synchronized Waypoint temporary() {
        for (Waypoint w : currentSet().all()) {
            if (w.temporary() || w.isDestination()) return w;
        }
        return null;
    }

    public synchronized void remove(Waypoint w) {
        for (WaypointSet s : sets.values()) {
            if (s.mutable().remove(w)) break;
        }
        saveSoon();
    }

    public synchronized void touch() { saveSoon(); }

    public synchronized List<Waypoint> visible() {
        List<Waypoint> out = new ArrayList<>();
        for (WaypointSet s : sets.values()) {
            if (s.name().equals(currentSet)) {
                out.addAll(s.all());
            } else {
                for (Waypoint w : s.all()) if (w.isGlobal() && !w.disabled()) out.add(w);
            }
        }
        return out;
    }

    public synchronized List<Waypoint> inCurrentSet() {
        return new ArrayList<>(currentSet().all());
    }

    public synchronized List<Waypoint> sortedForList(double px, double py, double pz, float yaw, String filter) {
        List<Waypoint> list = inCurrentSet();
        if (filter != null && !filter.isBlank()) {
            String f = filter.toLowerCase();
            list.removeIf(w -> !w.name().toLowerCase().contains(f) && !w.initials().toLowerCase().contains(f));
        }
        switch (sort) {
            case NAME -> list.sort((a, b) -> a.comparisonName().compareTo(b.comparisonName()));
            case COLOR -> list.sort((a, b) -> Integer.compare(a.color().ordinal(), b.color().ordinal()));
            case DISTANCE -> list.sort((a, b) -> Double.compare(a.distSq(px, py, pz), b.distSq(px, py, pz)));
            case ANGLE -> list.sort((a, b) -> Double.compare(angleDiff(a, px, pz, yaw), angleDiff(b, px, pz, yaw)));
            default -> {}
        }
        list.sort((a, b) -> Boolean.compare(a.isDeath(), b.isDeath()));
        return list;
    }

    private static double angleDiff(Waypoint w, double px, double pz, float yaw) {
        double dx = (w.x() + 0.5) - px;
        double dz = (w.z() + 0.5) - pz;
        double worldAngle = Math.atan2(-dx, dz);
        double rel = worldAngle - Math.toRadians(yaw);
        while (rel > Math.PI) rel -= Math.PI * 2;
        while (rel < -Math.PI) rel += Math.PI * 2;
        return Math.abs(rel);
    }

    private void saveSoon() {
        long now = System.currentTimeMillis();
        if (now - lastSaveMs > 2000) {
            saveNow();
        }
    }

    public synchronized void saveNow() {
        if (dimRoot == null) return;
        lastSaveMs = System.currentTimeMillis();
        WaypointStore.save(dimRoot, new WaypointStore.State(new LinkedHashMap<>(sets), currentSet));
    }

    public void tickDeathTracking(Minecraft mc) {
        if (mc.player == null || mc.level == null || dimRoot == null) { wasAlive = true; return; }
        boolean alive = mc.player.isAlive() && mc.player.getHealth() > 0;
        if (wasAlive && !alive) {
            int x = (int) Math.floor(mc.player.getX());
            int y = (int) Math.floor(mc.player.getY());
            int z = (int) Math.floor(mc.player.getZ());
            onDeath(x, y, z);
        }
        wasAlive = alive;
    }

    private synchronized void onDeath(int x, int y, int z) {
        WaypointSet set = currentSet();
        Waypoint old = null;
        for (Waypoint w : set.all()) {
            if (w.isDeath()) { old = w; break; }
        }
        if (old != null) set.mutable().remove(old);
        Waypoint death = new Waypoint("Deathpoint", "D", x, y, z, WaypointColor.RED);
        death.setKind(Waypoint.Kind.DEATH);
        death.setVisibility(Waypoint.Visibility.GLOBAL);
        set.addFirst(death);
        saveNow();
        AuraMapBridge.LOGGER.info("[auramap] death waypoint at {}, {}, {}", x, y, z);
    }

    public synchronized Waypoint findById(String id) {
        for (WaypointSet s : sets.values()) {
            for (Waypoint w : s.all()) if (w.id().equals(id)) return w;
        }
        return null;
    }

    public String worldLabel() { return worldLabel; }
}
