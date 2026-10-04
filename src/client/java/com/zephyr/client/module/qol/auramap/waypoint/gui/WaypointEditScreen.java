package com.zephyr.client.module.qol.auramap.waypoint.gui;

import com.zephyr.client.module.qol.auramap.waypoint.Waypoint;
import com.zephyr.client.module.qol.auramap.waypoint.WaypointColor;
import com.zephyr.client.module.qol.auramap.waypoint.WaypointManager;
import com.zephyr.client.module.qol.auramap.world.DimensionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WaypointEditScreen extends Screen {
    private final Screen parent;
    private final Waypoint editing;
    private final int preX, preY, preZ;

    private EditBox nameBox;
    private EditBox initialsBox;
    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;
    private EditBox yawBox;
    private WaypointColor color = WaypointColor.YELLOW;
    private Waypoint.Visibility visibility = Waypoint.Visibility.LOCAL;
    private String setName = "default";
    private int typeMode;
    private Button confirmButton;
    private boolean setExpanded;
    private boolean addingSet;
    private String newSetName = "";
    private EditBox newSetBox;

    public WaypointEditScreen(Screen parent, Waypoint editing, int preX, int preY, int preZ) {
        super(Component.literal(editing == null ? "New Waypoint" : "Edit Waypoint"));
        this.parent = parent;
        this.editing = editing;
        this.preX = preX;
        this.preY = preY;
        this.preZ = preZ;
        var mgr = WaypointManager.get();
        this.setName = mgr.currentSetName();
        if (editing != null) {
            this.color = editing.color();
            this.visibility = editing.visibility();
            if (editing.isDestination()) this.typeMode = 3;
            else if (editing.temporary()) this.typeMode = 2;
            else if (editing.disabled()) this.typeMode = 1;
            else this.typeMode = 0;
        } else {
            this.color = WaypointColor.LIME;
        }
    }

    @Override
    protected void init() {
        var mgr = WaypointManager.get();
        var font = Minecraft.getInstance().font;
        int cx = width / 2;
        int contentW = Math.min(660, width - 20);
        int x0 = cx - contentW / 2;

        int halfGap = 8;
        int halfW = (contentW - halfGap) / 2;
        addRenderableWidget(Button.builder(Component.literal(displayWorldName() + " (auto)"), b -> {})
                .bounds(x0, 26, halfW, 20).build());
        addRenderableWidget(Button.builder(Component.literal(displayDimName() + " (auto)"), b -> {})
                .bounds(x0 + halfW + halfGap, 26, halfW, 20).build());

        int presetW = Math.min(160, contentW / 3);
        addRenderableWidget(Button.builder(Component.literal("Preset"), b -> resetForm())
                .bounds(x0, 52, presetW, 20).build());
        Button setBtn = Button.builder(
                Component.literal(setName + (setExpanded ? " ▴" : " ▾")), b -> {
                    setExpanded = !setExpanded;
                    addingSet = false;
                    rebuildWidgets();
                }).bounds(x0 + presetW + 8, 52, contentW - presetW - 8, 20).build();
        setBtn.active = editing == null;
        addRenderableWidget(setBtn);

        int y = 78;
        if (setExpanded && editing == null) {
            for (var s : mgr.sets()) {
                final String nm = s.name();
                Button sb = Button.builder(Component.literal(nm + " (" + s.size() + ")"), b -> {
                    setName = nm;
                    setExpanded = false;
                    addingSet = false;
                    rebuildWidgets();
                }).bounds(x0, y, contentW, 20).build();
                sb.active = !nm.equals(setName);
                addRenderableWidget(sb);
                y += 22;
            }
            if (addingSet) {
                newSetBox = new EditBox(font, x0, y, contentW - 170, 20,
                        Component.literal("Set name"));
                newSetBox.setMaxLength(32);
                newSetBox.setValue(newSetName);
                newSetBox.setResponder(v -> newSetName = v);
                addRenderableWidget(newSetBox);
                addRenderableWidget(Button.builder(Component.literal("Create"), b -> {
                    String n = newSetName.trim();
                    if (n.isEmpty()) return;
                    setName = n;
                    newSetName = "";
                    addingSet = false;
                    setExpanded = false;
                    rebuildWidgets();
                }).bounds(x0 + contentW - 162, y, 78, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> {
                    addingSet = false;
                    rebuildWidgets();
                }).bounds(x0 + contentW - 80, y, 80, 20).build());
                y += 26;
            } else {
                addRenderableWidget(Button.builder(Component.literal("+ New set"), b -> {
                    addingSet = true;
                    rebuildWidgets();
                }).bounds(x0, y, contentW, 20).build());
                y += 26;
            }
        }

        addRenderableWidget(Button.builder(colorLabel(), b -> {
            WaypointColor[] vals = WaypointColor.values();
            color = vals[(color.ordinal() + 1) % vals.length];
            b.setMessage(colorLabel());
        }).bounds(cx - 130, y, 260, 20).build());
        y += 26;

        nameBox = new EditBox(font, x0, y, contentW, 20, Component.literal("Name"));
        nameBox.setMaxLength(64);
        nameBox.setValue(editing != null ? editing.name() : "");
        nameBox.setResponder(s -> updateConfirm());
        addRenderableWidget(nameBox);
        y += 26;

        int coordGap = 6;
        int coordW = (contentW - coordGap * 3) / 4;
        xBox = new EditBox(font, x0, y, coordW, 20, Component.literal("X"));
        yBox = new EditBox(font, x0 + (coordW + coordGap), y, coordW, 20, Component.literal("Y"));
        zBox = new EditBox(font, x0 + (coordW + coordGap) * 2, y, coordW, 20, Component.literal("Z"));
        yawBox = new EditBox(font, x0 + (coordW + coordGap) * 3, y, coordW, 20, Component.literal("yaw"));
        xBox.setValue(editing != null ? String.valueOf(editing.x()) : String.valueOf(preX));
        yBox.setValue(editing != null ? (editing.yIncluded() ? String.valueOf(editing.y()) : "~") : String.valueOf(preY));
        zBox.setValue(editing != null ? String.valueOf(editing.z()) : String.valueOf(preZ));
        yawBox.setValue(editing != null && editing.rotation() ? String.valueOf(editing.yaw()) : "");
        yawBox.setMaxLength(6);
        for (EditBox b : new EditBox[]{xBox, yBox, zBox, yawBox}) {
            b.setResponder(s -> splitOnSpaces());
            addRenderableWidget(b);
        }
        y += 26;

        int visW = 170, initW = 120, typeW = 170, flagGap = 8;
        int flagsW = visW + initW + typeW + flagGap * 2;
        int fx = cx - flagsW / 2;
        addRenderableWidget(CycleButton.booleanBuilder(Component.literal("Global"), Component.literal("Local"),
                visibility == Waypoint.Visibility.GLOBAL)
                .displayOnlyValue()
                .create(fx, y, visW, 20, Component.literal("Visibility"),
                        (btn, v) -> visibility = v ? Waypoint.Visibility.GLOBAL : Waypoint.Visibility.LOCAL));
        initialsBox = new EditBox(font, fx + visW + flagGap, y, initW, 20,
                Component.literal("initials"));
        initialsBox.setMaxLength(2);
        initialsBox.setValue(editing != null ? editing.initials() : "");
        initialsBox.setResponder(s -> {
            if (s.length() > 2) initialsBox.setValue(s.substring(0, 2));
            updateConfirm();
        });
        addRenderableWidget(initialsBox);
        addRenderableWidget(Button.builder(typeLabel(), b -> {
            typeMode = (typeMode + 1) % 4;
            b.setMessage(typeLabel());
        }).bounds(fx + visW + flagGap + initW + flagGap, y, typeW, 20).build());

        int bottomGap = 16;
        int bottomW = (contentW - bottomGap) / 2;
        int by = Math.max(y + 30, height - 34);
        confirmButton = Button.builder(Component.literal("Confirm"), b -> save())
                .bounds(x0, by, bottomW, 20).build();
        addRenderableWidget(confirmButton);
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(x0 + bottomW + bottomGap, by, bottomW, 20).build());
        updateConfirm();
    }

    private Component colorLabel() {
        return Component.literal(color.label()).withStyle(s -> s.withColor(color.argb()));
    }

    private Component typeLabel() {
        return switch (typeMode) {
            case 1 -> Component.literal("Disabled");
            case 2 -> Component.literal("Temporary");
            case 3 -> Component.literal("Destination");
            default -> Component.literal("Enabled");
        };
    }

    private void resetForm() {
        var mc = Minecraft.getInstance();
        int x = mc.player != null ? (int) Math.floor(mc.player.getX()) : preX;
        int y = mc.player != null ? (int) Math.floor(mc.player.getY()) : preY;
        int z = mc.player != null ? (int) Math.floor(mc.player.getZ()) : preZ;
        nameBox.setValue("");
        initialsBox.setValue("");
        xBox.setValue(String.valueOf(x));
        yBox.setValue(String.valueOf(y));
        zBox.setValue(String.valueOf(z));
        yawBox.setValue("");
        updateConfirm();
    }

    private void splitOnSpaces() {
        spread(xBox, yBox);
        spread(yBox, zBox);
        spread(zBox, yawBox);
    }

    private static void spread(EditBox from, EditBox to) {
        String v = from.getValue();
        int sp = v.indexOf(' ');
        if (sp < 0) return;
        from.setValue(v.substring(0, sp));
        to.setValue(to.getValue() + v.substring(sp + 1));
    }

    private void updateConfirm() {
        if (confirmButton == null) return;
        confirmButton.active = nameBox != null && !nameBox.getValue().isBlank();
    }

    private void save() {
        if (nameBox.getValue().isBlank()) return;
        String yText = yBox.getValue().trim();
        boolean yInc = !yText.equals("~") && !yText.isEmpty();
        int x = parse(xBox.getValue(), preX);
        int yy = yInc ? parse(yText, preY) : preY;
        int z = parse(zBox.getValue(), preZ);
        String yawText = yawBox.getValue().trim();
        boolean useYaw = !yawText.isEmpty() && !yawText.equals("-");
        int yaw = 0;
        if (useYaw) {
            try { yaw = Integer.parseInt(yawText); } catch (Exception ignored) { useYaw = false; }
        }
        String name = nameBox.getValue().trim();
        String initials = initialsBox.getValue().trim();
        if (initials.isEmpty() && !name.isEmpty()) initials = name.substring(0, 1).toUpperCase();
        if (initials.length() > 2) initials = initials.substring(0, 2);

        if (editing != null) {
            editing.setName(name);
            editing.setInitials(initials);
            editing.setPos(x, yy, z);
            editing.setColor(color);
            editing.setVisibility(visibility);
            editing.setYIncluded(yInc);
            editing.setRotation(useYaw);
            if (useYaw) editing.setYaw(yaw);
            applyType(editing);
            WaypointManager.get().touch();
        } else {
            Waypoint w = new Waypoint(name, initials, x, yy, z, color);
            w.setVisibility(visibility);
            w.setYIncluded(yInc);
            w.setRotation(useYaw);
            if (useYaw) w.setYaw(yaw);
            applyType(w);
            var mgr = WaypointManager.get();
            if (!setName.equals(mgr.currentSetName())) mgr.switchSet(setName);
            mgr.add(w);
        }
        onClose();
    }

    private void applyType(Waypoint w) {
        switch (typeMode) {
            case 1 -> { w.setKind(Waypoint.Kind.NORMAL); w.setDisabled(true); }
            case 2 -> { w.setKind(Waypoint.Kind.NORMAL); w.setTemporary(true); }
            case 3 -> { w.setDisabled(false); w.setTemporary(false); w.setKind(Waypoint.Kind.DESTINATION); }
            default -> { w.setKind(Waypoint.Kind.NORMAL); w.setDisabled(false); w.setTemporary(false); }
        }
    }

    private static int parse(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return fallback; }
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
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        var font = Minecraft.getInstance().font;
        g.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        if (initialsBox != null && initialsBox.getValue().isEmpty() && !initialsBox.isFocused()) {
            g.text(font, "initials", initialsBox.getX() + 4, initialsBox.getY() + 6, 0xFF707070, false);
        }
        if (yawBox != null && yawBox.getValue().isEmpty() && !yawBox.isFocused()) {
            g.text(font, "yaw", yawBox.getX() + 4, yawBox.getY() + 6, 0xFF707070, false);
        }
        if (newSetBox != null && newSetBox.getValue().isEmpty() && !newSetBox.isFocused()) {
            g.text(font, "Set name", newSetBox.getX() + 4, newSetBox.getY() + 6, 0xFF707070, false);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreenAndShow(parent);
    }
}
