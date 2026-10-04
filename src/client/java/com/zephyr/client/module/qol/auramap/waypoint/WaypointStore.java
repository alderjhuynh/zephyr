package com.zephyr.client.module.qol.auramap.waypoint;

import com.zephyr.client.module.qol.auramap.AuraMapBridge;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WaypointStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "waypoints.json";

    private WaypointStore() {}

    public record State(Map<String, WaypointSet> sets, String currentSet) {}

    public static Path fileFor(Path dimRoot) { return dimRoot.resolve(FILE); }

    public static State load(Path dimRoot) {
        Map<String, WaypointSet> sets = new LinkedHashMap<>();
        String current = "default";
        Path f = fileFor(dimRoot);
        if (Files.exists(f)) {
            try {
                String json = Files.readString(f);
                JsonObject root = GSON.fromJson(json, JsonObject.class);
                if (root != null) {
                    if (root.has("currentSet")) current = root.get("currentSet").getAsString();
                    if (root.has("sets")) {
                        for (JsonElement e : root.getAsJsonArray("sets")) {
                            JsonObject o = e.getAsJsonObject();
                            WaypointSet set = new WaypointSet(o.has("name") ? o.get("name").getAsString() : "default");
                            if (o.has("waypoints")) {
                                for (JsonElement w : o.getAsJsonArray("waypoints")) {
                                    Waypoint wp = fromJson(w.getAsJsonObject());
                                    if (wp != null) set.add(wp);
                                }
                            }
                            sets.put(set.name(), set);
                        }
                    }
                }
            } catch (Exception e) {
                AuraMapBridge.LOGGER.warn("[auramap] failed to load waypoints {}", f, e);
            }
        }
        if (sets.isEmpty()) {
            List<ImportedWaypoint> imported = tryImportXaero(dimRoot);
            WaypointSet def = new WaypointSet("default");
            String cur = "default";
            Map<String, WaypointSet> bySet = new LinkedHashMap<>();
            bySet.put("default", def);
            for (ImportedWaypoint iw : imported) {
                WaypointSet s = bySet.computeIfAbsent(iw.set(), WaypointSet::new);
                s.add(iw.wp());
            }
            if (!imported.isEmpty()) {
                sets.putAll(bySet);
                var st = new State(sets, cur);
                save(dimRoot, st);
                return st;
            }
            sets.put("default", def);
        }
        if (!sets.containsKey(current)) current = sets.keySet().iterator().next();
        return new State(sets, current);
    }

    public static void save(Path dimRoot, State state) {
        try {
            Files.createDirectories(dimRoot);
            JsonObject root = new JsonObject();
            root.addProperty("currentSet", state.currentSet());
            JsonArray arr = new JsonArray();
            for (WaypointSet s : state.sets().values()) {
                JsonObject o = new JsonObject();
                o.addProperty("name", s.name());
                JsonArray wps = new JsonArray();
                for (Waypoint w : s.all()) {
                    if (w.temporary()) continue;
                    wps.add(toJson(w));
                }
                o.add("waypoints", wps);
                arr.add(o);
            }
            root.add("sets", arr);
            Path tmp = dimRoot.resolve(FILE + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, fileFor(dimRoot), java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            AuraMapBridge.LOGGER.warn("[auramap] failed to save waypoints to {}", dimRoot, e);
        }
    }

    private static JsonObject toJson(Waypoint w) {
        JsonObject o = new JsonObject();
        o.addProperty("name", w.name());
        o.addProperty("initials", w.initials());
        o.addProperty("x", w.x());
        o.addProperty("y", w.y());
        o.addProperty("z", w.z());
        o.addProperty("yIncluded", w.yIncluded());
        o.addProperty("color", w.color().name());
        o.addProperty("visibility", w.visibility().name());
        o.addProperty("disabled", w.disabled());
        o.addProperty("kind", w.kind().name());
        o.addProperty("rotation", w.rotation());
        o.addProperty("yaw", w.yaw());
        return o;
    }

    private static Waypoint fromJson(JsonObject o) {
        try {
            String name = o.has("name") ? o.get("name").getAsString() : "Waypoint";
            String initials = o.has("initials") ? o.get("initials").getAsString() : "";
            int x = o.get("x").getAsInt();
            int y = o.has("y") ? o.get("y").getAsInt() : 64;
            int z = o.get("z").getAsInt();
            Waypoint wp = new Waypoint(name, initials, x, y, z,
                    WaypointColor.fromName(o.has("color") ? o.get("color").getAsString() : "YELLOW", WaypointColor.YELLOW));
            if (o.has("yIncluded")) wp.setYIncluded(o.get("yIncluded").getAsBoolean());
            if (o.has("visibility")) {
                try { wp.setVisibility(Waypoint.Visibility.valueOf(o.get("visibility").getAsString())); } catch (Exception ignored) {}
            }
            if (o.has("disabled")) wp.setDisabled(o.get("disabled").getAsBoolean());
            if (o.has("kind")) {
                try { wp.setKind(Waypoint.Kind.valueOf(o.get("kind").getAsString())); } catch (Exception ignored) {}
            }
            if (o.has("rotation")) try { wp.setRotation(o.get("rotation").getAsBoolean()); } catch (Exception ignored) {}
            if (o.has("yaw")) try { wp.setYaw(o.get("yaw").getAsInt()); } catch (Exception ignored) {}
            return wp;
        } catch (Exception e) {
            return null;
        }
    }

    public record ImportedWaypoint(Waypoint wp, String set) {}

    static List<ImportedWaypoint> tryImportXaero(Path dimRoot) {
        List<ImportedWaypoint> out = new ArrayList<>();
        try {
            if (Files.isDirectory(dimRoot)) {
                try (var s = Files.list(dimRoot)) {
                    for (Path p : (Iterable<Path>) s::iterator) {
                        if (p.getFileName().toString().endsWith(".txt")) {
                            out.addAll(parseXaeroTxt(p));
                        }
                    }
                }
            }
        } catch (IOException ignored) {}
        return out;
    }

    static List<ImportedWaypoint> parseXaeroTxt(Path file) {
        List<ImportedWaypoint> out = new ArrayList<>();
        List<String> lines;
        try { lines = Files.readAllLines(file, StandardCharsets.UTF_8); }
        catch (IOException e) { return out; }
        String currentSet = "default";
        for (String line : lines) {
            if (line.startsWith("sets:")) {
                String[] parts = line.split(":", -1);
                if (parts.length > 1 && !parts[1].isBlank()) currentSet = parts[1];
                continue;
            }
            if (!line.startsWith("waypoint:")) continue;
            String[] a = line.split(":", -1);
            if (a.length < 10) continue;
            try {
                String name = unescape(a[1]);
                String initials = unescape(a[2]);
                int x = Integer.parseInt(a[3].trim());
                boolean yInc = !a[4].trim().equals("~");
                int y = yInc ? Integer.parseInt(a[4].trim()) : 64;
                int z = Integer.parseInt(a[5].trim());
                int colorIdx = Integer.parseInt(a[6].trim());
                boolean disabled = a[7].trim().equals("true");
                int type = Integer.parseInt(a[8].trim());
                String set = a[9].isBlank() ? currentSet : a[9];
                Waypoint wp = new Waypoint(name, initials, x, y, z, WaypointColor.fromIndex(colorIdx));
                wp.setYIncluded(yInc);
                wp.setDisabled(disabled);
                if (type == 3) wp.setKind(Waypoint.Kind.DESTINATION);
                else if (type == 1 || type == 2) wp.setKind(Waypoint.Kind.DEATH);
                if (a.length > 12) {
                    String vis = a[12].trim();
                    if (vis.equals("true") || vis.equalsIgnoreCase("GLOBAL")) wp.setVisibility(Waypoint.Visibility.GLOBAL);
                }
                out.add(new ImportedWaypoint(wp, set.isBlank() ? "default" : set));
            } catch (Exception ignored) {}
        }
        return out;
    }

    private static String unescape(String s) { return s.replace("\u00a7\u00a7", ":"); }
}
