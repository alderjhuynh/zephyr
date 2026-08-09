package com.zephyr.client.configplusgui.screen;

import com.zephyr.client.configplusgui.keybind.GlfwKeyNames;
import com.zephyr.client.configplusgui.keybind.Keybind;
import com.zephyr.client.configplusgui.keybind.KeybindManager;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class KeybindGuiScreen extends ZephyrScreen {
    private static final int ROW_HEIGHT = 24;
    private static final int SECTION_GAP = 6;
    private static final int CLEAR_BOX_SIZE = 12;
    private static final int CONFLICT_RED = 0xFFE05B5B;

    private final LinkedHashSet<Integer> captureBuffer = new LinkedHashSet<>();
    private final Set<Integer> currentlyHeld = new java.util.HashSet<>();
    private Object capturingTarget = null; // Module or KeybindManager.SystemAction

    private double scrollOffset = 0;

    public KeybindGuiScreen() {
        this(0);
    }

    KeybindGuiScreen(int enterDirection) {
        super(Component.literal("Zephyr"), enterDirection);
    }

    @Override
    protected Nav currentNav() {
        return Nav.KEYBIND;
    }

    public boolean isCapturing() {
        return capturingTarget != null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        withPanelSlide(() -> {
            renderChrome(graphics, mouseX, mouseY);

            int listTop = panelY + headerHeight();
            int listBottom = panelY + panelHeight - PADDING;

            List<Row> rows = computeRows();
            int contentHeight = rows.isEmpty() ? 0 : rows.get(rows.size() - 1).bottom - listTop;
            int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            graphics.enableScissor(panelX, listTop, panelX + panelWidth, listBottom);
            for (Row row : rows) {
                renderRow(graphics, row, (int) scrollOffset, mouseX, mouseY);
            }
            graphics.disableScissor();

            super.render(graphics, mouseX, mouseY, partialTick);
        });
    }

    private void renderRow(GuiGraphics graphics, Row row, int scroll, int mouseX, int mouseY) {
        int top = row.top - scroll;
        boolean capturing = row.target.equals(capturingTarget);
        boolean hovered = mouseX >= panelX + PADDING && mouseX <= panelX + panelWidth - PADDING
                && mouseY >= top && mouseY < top + ROW_HEIGHT;

        int bg = capturing ? rowBgEnabled() : (hovered ? ROW_BG_HOVER : ROW_BG);
        graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + ROW_HEIGHT, bg);

        graphics.drawString(this.font, row.label, panelX + PADDING + 8, top + 9, TEXT_MAIN, false);

        String valueText = capturing ? captureLabel() : row.bind.getLabel();
        int valueColor;
        if (capturing) {
            valueColor = accent();
        } else if (row.bind.conflicted()) {
            valueColor = CONFLICT_RED;
        } else {
            valueColor = row.bind.isSet() ? accent() : TEXT_DIM;
        }

        boolean showClear = !capturing && row.bind.isSet();
        int valueRight = panelX + panelWidth - PADDING - 8 - (showClear ? CLEAR_BOX_SIZE + 6 : 0);
        int valueWidth = this.font.width(valueText);
        graphics.drawString(this.font, valueText, valueRight - valueWidth, top + 9, valueColor, false);

        if (showClear) {
            int clearX = panelX + panelWidth - PADDING - CLEAR_BOX_SIZE;
            int clearY = top + (ROW_HEIGHT - CLEAR_BOX_SIZE) / 2;
            boolean clearHovered = mouseX >= clearX && mouseX < clearX + CLEAR_BOX_SIZE
                    && mouseY >= clearY && mouseY < clearY + CLEAR_BOX_SIZE;
            graphics.fill(clearX, clearY, clearX + CLEAR_BOX_SIZE, clearY + CLEAR_BOX_SIZE,
                    clearHovered ? 0xFFE05B5B : 0x40FFFFFF);
            graphics.drawCenteredString(this.font, "x", clearX + CLEAR_BOX_SIZE / 2, clearY + 1, TEXT_DIM);
        }
    }

    private String captureLabel() {
        if (captureBuffer.isEmpty()) return "Press keys...";
        StringBuilder builder = new StringBuilder();
        for (int key : captureBuffer) {
            if (!builder.isEmpty()) builder.append(" + ");
            builder.append(GlfwKeyNames.label(key));
        }
        return builder.toString();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (button != 0) return false;

        int scroll = (int) scrollOffset;
        for (Row row : computeRows()) {
            int top = row.top - scroll;
            if (mouseY < top || mouseY >= top + ROW_HEIGHT) continue;
            if (mouseX < panelX + PADDING || mouseX > panelX + panelWidth - PADDING) continue;

            boolean capturing = row.target.equals(capturingTarget);
            boolean showClear = !capturing && row.bind.isSet();
            if (showClear) {
                int clearX = panelX + panelWidth - PADDING - CLEAR_BOX_SIZE;
                int clearY = top + (ROW_HEIGHT - CLEAR_BOX_SIZE) / 2;
                if (mouseX >= clearX && mouseX < clearX + CLEAR_BOX_SIZE
                        && mouseY >= clearY && mouseY < clearY + CLEAR_BOX_SIZE) {
                    clearBind(row.target);
                    return true;
                }
            }

            startCapture(row.target);
            return true;
        }

        return false;
    }

    private void startCapture(Object target) {
        capturingTarget = target;
        captureBuffer.clear();
        currentlyHeld.clear();
    }

    private void cancelCapture() {
        capturingTarget = null;
        captureBuffer.clear();
        currentlyHeld.clear();
    }

    private void commitCapture() {
        Keybind bind = Keybind.of(new ArrayList<>(captureBuffer));
        if (capturingTarget instanceof Module module) {
            KeybindManager.set(module, bind);
        } else if (capturingTarget instanceof KeybindManager.SystemAction action) {
            KeybindManager.set(action, bind);
        }
        cancelCapture();
    }

    private void clearBind(Object target) {
        if (target instanceof Module module) {
            KeybindManager.clear(module);
        } else if (target instanceof KeybindManager.SystemAction action) {
            KeybindManager.set(action, Keybind.NONE);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capturingTarget != null) {
            int key = event.key();
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                cancelCapture();
                return true;
            }
            currentlyHeld.add(key);
            if (captureBuffer.size() < Keybind.MAX_KEYS) {
                captureBuffer.add(key);
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (capturingTarget != null) {
            currentlyHeld.remove(event.key());
            if (currentlyHeld.isEmpty() && !captureBuffer.isEmpty()) {
                commitCapture();
            }
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= scrollY * ROW_HEIGHT;
        return true;
    }

    private List<Row> computeRows() {
        List<Row> rows = new ArrayList<>();
        int cursor = panelY + headerHeight();

        for (KeybindManager.SystemAction action : KeybindManager.SystemAction.values()) {
            rows.add(new Row(action, action.label, KeybindManager.get(action), cursor));
            cursor += ROW_HEIGHT;
        }

        cursor += SECTION_GAP;

        for (Module module : ModuleManager.getModules()) {
            rows.add(new Row(module, module.getName(), KeybindManager.get(module), cursor));
            cursor += ROW_HEIGHT;
        }

        return rows;
    }

    private static final class Row {
        final Object target;
        final String label;
        final Keybind bind;
        final int top;
        final int bottom;

        Row(Object target, String label, Keybind bind, int top) {
            this.target = target;
            this.label = label;
            this.bind = bind;
            this.top = top;
            this.bottom = top + ROW_HEIGHT;
        }
    }
}