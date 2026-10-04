package com.zephyr.client.module.qol.auramap.waypoint.gui;

import com.zephyr.client.module.qol.auramap.waypoint.Waypoint;
import com.zephyr.client.module.qol.auramap.waypoint.WaypointManager;
import com.zephyr.client.module.qol.auramap.waypoint.WaypointShare;
import com.zephyr.client.module.qol.auramap.waypoint.WaypointTeleport;
import com.zephyr.client.module.qol.auramap.waypoint.WaypointSet;
import com.zephyr.client.module.qol.auramap.world.DimensionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class WaypointsScreen extends Screen {
    private final Screen parent;
    private int scroll;
    private EditBox filterBox;
    private String filter = "";
    private Waypoint selected;
    private boolean setsExpanded;
    private boolean addingSet;
    private String newSetName = "";
    private EditBox newSetBox;
    private String pendingDeleteSet;
    private int listTop = 94;
    private int listBottom;
    private int contentX0;
    private int contentW;

    public WaypointsScreen(Screen parent) {
        super(Component.literal("Waypoints"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var mgr = WaypointManager.get();
        var mc = Minecraft.getInstance();
        var font = mc.font;
        int cx = width / 2;
        contentW = Math.min(540, width - 16);
        contentX0 = cx - contentW / 2;

        int halfGap = 4;
        int halfW = (contentW - halfGap) / 2;
        addRenderableWidget(Button.builder(Component.literal(displayWorldName() + " (auto)"), b -> {})
                .bounds(contentX0, 20, halfW, 20).build());
        addRenderableWidget(Button.builder(Component.literal(displayDimName() + " (auto)"), b -> {})
                .bounds(contentX0 + halfW + halfGap, 20, halfW, 20).build());

        int clearW = Math.max(80, Math.min(120, (int) (contentW * 0.22)));
        int setW = contentW - clearW - 4;
        addRenderableWidget(Button.builder(
                Component.literal(mgr.currentSetName() + (setsExpanded ? " ▴" : " ▾")), b -> {
                    setsExpanded = !setsExpanded;
                    addingSet = false;
                    pendingDeleteSet = null;
                    rebuildWidgets();
                }).bounds(contentX0, 44, setW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear Set"), b -> {
            mgr.clearCurrentSet();
            selected = null;
            scroll = 0;
            rebuildWidgets();
        }).bounds(contentX0 + setW + 4, 44, clearW, 20).build());

        int y = 68;
        if (setsExpanded) {
            String cur = mgr.currentSetName();
            boolean canDelete = mgr.sets().size() > 1;
            for (WaypointSet s : mgr.sets()) {
                final String nm = s.name();
                Button sb = Button.builder(Component.literal(nm + " (" + s.size() + ")"), b -> {
                    mgr.switchSet(nm);
                    setsExpanded = false;
                    addingSet = false;
                    pendingDeleteSet = null;
                    scroll = 0;
                    rebuildWidgets();
                }).bounds(contentX0, y, contentW - 28, 20).build();
                sb.active = !nm.equals(cur);
                addRenderableWidget(sb);
                String delLabel = nm.equals(pendingDeleteSet) ? "?" : "✕";
                Button del = Button.builder(Component.literal(delLabel), b -> {
                    if (nm.equals(pendingDeleteSet)) {
                        mgr.removeSet(nm);
                        pendingDeleteSet = null;
                        if (selected != null
                                && WaypointManager.get().findById(selected.id()) == null) {
                            selected = null;
                        }
                        scroll = 0;
                        rebuildWidgets();
                    } else {
                        pendingDeleteSet = nm;
                        rebuildWidgets();
                    }
                }).bounds(contentX0 + contentW - 24, y, 24, 20).build();
                del.active = canDelete;
                addRenderableWidget(del);
                y += 22;
            }
            if (addingSet) {
                newSetBox = new EditBox(font, contentX0, y, contentW - 170, 20,
                        Component.literal("Set name"));
                newSetBox.setMaxLength(32);
                newSetBox.setValue(newSetName);
                newSetBox.setResponder(v -> newSetName = v);
                addRenderableWidget(newSetBox);
                addRenderableWidget(Button.builder(Component.literal("Create"), b -> {
                    String n = newSetName.trim();
                    if (n.isEmpty()) return;
                    mgr.switchSet(n);
                    newSetName = "";
                    addingSet = false;
                    setsExpanded = false;
                    pendingDeleteSet = null;
                    scroll = 0;
                    rebuildWidgets();
                }).bounds(contentX0 + contentW - 162, y, 78, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> {
                    addingSet = false;
                    rebuildWidgets();
                }).bounds(contentX0 + contentW - 80, y, 80, 20).build());
                y += 26;
            } else {
                addRenderableWidget(Button.builder(Component.literal("+ New set"), b -> {
                    addingSet = true;
                    rebuildWidgets();
                }).bounds(contentX0, y, contentW, 20).build());
                y += 26;
            }
        }

        filterBox = new EditBox(font, contentX0, y, Math.min(200, contentW / 2), 20,
                Component.literal("Filter..."));
        filterBox.setValue(filter);
        filterBox.setResponder(s -> {
            filter = s;
            scroll = 0;
            rebuildWidgets();
        });
        addRenderableWidget(filterBox);

        listTop = y + 26;
        listBottom = height - 62;
        List<Waypoint> list = currentList();
        if (selected != null && !list.contains(selected)
                && WaypointManager.get().findById(selected.id()) == null) {
            selected = null;
        }
        int rowH = 24;
        int visible = Math.max(1, (listBottom - listTop) / rowH);
        for (int i = 0; i < visible; i++) {
            int idx = scroll + i;
            if (idx >= list.size()) break;
            Waypoint w = list.get(idx);
            int ry = listTop + i * rowH;
            final Waypoint ref = w;
            addRenderableWidget(Button.builder(Component.literal(rowName(w)), b -> {
                selected = ref;
                rebuildWidgets();
            }).bounds(contentX0, ry, contentW, 20).build());
        }

        int by = height - 56;
        int gap = 4;
        int bw = (contentW - gap * 4) / 5;
        int bx = contentX0;
        addRenderableWidget(Button.builder(Component.literal("Add/Edit"), b -> openAddOrEdit())
                .bounds(bx, by, bw, 20).build());
        bx += bw + gap;
        Button tp = Button.builder(Component.literal("Teleport (T)"), b -> {
            if (selected != null) {
                WaypointTeleport.teleportTo(selected);
                onClose();
            }
        }).bounds(bx, by, bw, 20).build();
        tp.active = selected != null;
        addRenderableWidget(tp);
        bx += bw + gap;
        Button share = Button.builder(Component.literal("Share"), b -> {
            if (selected != null) WaypointShare.share(selected);
        }).bounds(bx, by, bw, 20).build();
        share.active = selected != null;
        addRenderableWidget(share);
        bx += bw + gap;
        String toggleLabel = "Enable";
        if (selected != null && !selected.disabled()) toggleLabel = "Disable";
        Button toggle = Button.builder(Component.literal(toggleLabel), b -> {
            if (selected != null) {
                selected.setDisabled(!selected.disabled());
                WaypointManager.get().touch();
                rebuildWidgets();
            }
        }).bounds(bx, by, bw, 20).build();
        toggle.active = selected != null;
        addRenderableWidget(toggle);
        bx += bw + gap;
        Button del = Button.builder(Component.literal("Delete"), b -> {
            if (selected != null) {
                WaypointManager.get().remove(selected);
                selected = null;
                rebuildWidgets();
            }
        }).bounds(bx, by, bw, 20).build();
        del.active = selected != null;
        addRenderableWidget(del);

        int doneW = Math.min(330, contentW);
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx - doneW / 2, height - 30, doneW, 20).build());
    }

    private void openAddOrEdit() {
        var mc = Minecraft.getInstance();
        if (selected != null) {
            mc.setScreenAndShow(new WaypointEditScreen(this, selected,
                    selected.x(), selected.y(), selected.z()));
        } else {
            int x = mc.player != null ? (int) Math.floor(mc.player.getX()) : 0;
            int y = mc.player != null ? (int) Math.floor(mc.player.getY()) : 64;
            int z = mc.player != null ? (int) Math.floor(mc.player.getZ()) : 0;
            mc.setScreenAndShow(new WaypointEditScreen(this, null, x, y, z));
        }
    }

    private List<Waypoint> currentList() {
        var mc = Minecraft.getInstance();
        double px = mc.player != null ? mc.player.getX() : 0;
        double py = mc.player != null ? mc.player.getY() : 64;
        double pz = mc.player != null ? mc.player.getZ() : 0;
        float yaw = mc.player != null ? mc.player.getYRot() : 0;
        List<Waypoint> list = WaypointManager.get().sortedForList(px, py, pz, yaw, filter);
        int maxScroll = Math.max(0, list.size() - 1);
        if (scroll > maxScroll) scroll = maxScroll;
        return list;
    }

    private String rowName(Waypoint w) {
        String s = w.name();
        int max = 40;
        if (s.length() > max) s = s.substring(0, max - 1) + "…";
        return s;
    }

    private static String displayWorldName() {
        try {
            return DimensionContext.worldId();
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String displayDimName() {
        try {
            var mc = Minecraft.getInstance();
            if (mc.level != null) return mc.level.dimension().identifier().getPath();
        } catch (Throwable ignored) {}
        return "unknown";
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0) scroll++;
        else if (scrollY > 0) scroll = Math.max(0, scroll - 1);
        rebuildWidgets();
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 62, width, height - 62, 0x99000000);
        super.extractRenderState(g, mouseX, mouseY, delta);
        var font = Minecraft.getInstance().font;
        int halfW = (contentW - 4) / 2;
        g.centeredText(font, "World/Server", contentX0 + halfW / 2, 9, 0xFFFFFFFF);
        g.centeredText(font, "Sub-World/Dimension",
                contentX0 + halfW + 4 + halfW / 2, 9, 0xFFFFFFFF);
        if (filterBox != null && filterBox.getValue().isEmpty() && !filterBox.isFocused()) {
            g.text(font, "Filter...", filterBox.getX() + 4, filterBox.getY() + 6, 0xFF707070, false);
        }
        if (newSetBox != null && newSetBox.getValue().isEmpty() && !newSetBox.isFocused()) {
            g.text(font, "Set name", newSetBox.getX() + 4, newSetBox.getY() + 6, 0xFF707070, false);
        }
        try {
            List<Waypoint> list = currentList();
            int rowH = 24;
            int visible = Math.max(1, (listBottom - listTop) / rowH);
            for (int i = 0; i < visible; i++) {
                int idx = scroll + i;
                if (idx >= list.size()) break;
                Waypoint w = list.get(idx);
                int ry = listTop + i * rowH;
                String name = rowName(w);
                int tw = font.width(name);
                int box = 14;
                int iconX = width / 2 - tw / 2 - box - 8;
                int iconY = ry + 3;
                g.fill(iconX, iconY, iconX + box, iconY + box, 0xFF000000);
                g.fill(iconX + 1, iconY + 1, iconX + box - 1, iconY + box - 1,
                        w.disabled() ? 0xFF555555 : w.color().argb());
                String initial = w.initials();
                if (initial == null || initial.isEmpty()) {
                    initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
                }
                if (initial.length() > 2) initial = initial.substring(0, 2);
                int iw = font.width(initial);
                g.text(font, initial, iconX + (box - iw) / 2, iconY + 3, 0xFFFFFFFF, true);
                if (w == selected || (selected != null && w.id().equals(selected.id()))) {
                    g.outline(contentX0, ry, contentW, 20, 0xFFFFFFFF);
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreenAndShow(parent);
        else super.onClose();
    }
}
