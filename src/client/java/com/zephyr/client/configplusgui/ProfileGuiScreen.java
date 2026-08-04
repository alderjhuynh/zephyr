package com.zephyr.client.configplusgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ProfileGuiScreen extends ZephyrScreen {
    private static final int NEW_PROFILE_ROW_HEIGHT = 22;
    private static final int ROW_HEIGHT = 24;
    private static final int ICON_BOX_SIZE = 12;
    private static final int ICON_GAP = 6;

    private EditBox newProfileEditBox;
    private EditBox renameEditBox;
    private String renamingProfile = null;

    private double scrollOffset = 0;

    public ProfileGuiScreen() {
        this(0);
    }

    ProfileGuiScreen(int enterDirection) {
        super(Component.literal("Zephyr"), enterDirection);
    }

    @Override
    protected Nav currentNav() {
        return Nav.PROFILES;
    }

    @Override
    protected int headerHeight() {
        return TITLE_HEIGHT + NEW_PROFILE_ROW_HEIGHT;
    }

    @Override
    protected void initWidgets() {
        int boxY = panelY + TITLE_HEIGHT + 2;
        newProfileEditBox = new EditBox(this.font,
                panelX + PADDING, boxY, panelWidth - PADDING * 2, 16, Component.literal("New profile"));
        newProfileEditBox.setHint(Component.literal("New profile name, Enter to create..."));
        newProfileEditBox.setBordered(false);
        this.addRenderableWidget(newProfileEditBox);

        renamingProfile = null;
        renameEditBox = null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        withPanelSlide(() -> {
            newProfileEditBox.setX(panelX + PADDING);
            newProfileEditBox.setY(panelY + TITLE_HEIGHT + 2);
            if (renameEditBox != null) {
                Row renamingRow = findRow(renamingProfile);
                if (renamingRow != null) {
                    renameEditBox.setX(panelX + PADDING + 8);
                    renameEditBox.setY(renamingRow.top - (int) scrollOffset + 7);
                }
            }

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

            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        });
    }

    private void renderRow(GuiGraphicsExtractor graphics, Row row, int scroll, int mouseX, int mouseY) {
        int top = row.top - scroll;
        boolean active = row.name.equals(ProfileManager.getActiveProfile());
        boolean renaming = row.name.equals(renamingProfile);
        boolean hovered = mouseX >= panelX + PADDING && mouseX <= panelX + panelWidth - PADDING
                && mouseY >= top && mouseY < top + ROW_HEIGHT;

        int bg = active ? ROW_BG_ENABLED : (hovered ? ROW_BG_HOVER : ROW_BG);
        graphics.fill(panelX + PADDING, top, panelX + panelWidth - PADDING, top + ROW_HEIGHT, bg);

        boolean showDelete = !renaming && ProfileManager.getProfileNames().size() > 1;
        int iconsRight = panelX + panelWidth - PADDING - 8;
        int deleteX = iconsRight - ICON_BOX_SIZE;
        int renameX = deleteX - (showDelete ? ICON_BOX_SIZE + ICON_GAP : 0);

        if (!renaming) {
            int nameColor = active ? ACCENT : TEXT_MAIN;
            graphics.text(this.font, row.name, panelX + PADDING + 8, top + 9, nameColor, false);
        }

        int renameY = top + (ROW_HEIGHT - ICON_BOX_SIZE) / 2;
        boolean renameHovered = mouseX >= renameX && mouseX < renameX + ICON_BOX_SIZE
                && mouseY >= renameY && mouseY < renameY + ICON_BOX_SIZE;
        graphics.fill(renameX, renameY, renameX + ICON_BOX_SIZE, renameY + ICON_BOX_SIZE,
                renaming ? ACCENT : (renameHovered ? ROW_BG_HOVER : 0x40FFFFFF));
        graphics.centeredText(this.font, "R", renameX + ICON_BOX_SIZE / 2, renameY + 1,
                renaming ? TEXT_ON_ACCENT : TEXT_DIM);

        if (showDelete) {
            boolean deleteHovered = mouseX >= deleteX && mouseX < deleteX + ICON_BOX_SIZE
                    && mouseY >= renameY && mouseY < renameY + ICON_BOX_SIZE;
            graphics.fill(deleteX, renameY, deleteX + ICON_BOX_SIZE, renameY + ICON_BOX_SIZE,
                    deleteHovered ? 0xFFE05B5B : 0x40FFFFFF);
            graphics.centeredText(this.font, "x", deleteX + ICON_BOX_SIZE / 2, renameY + 1, TEXT_DIM);
        }
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

            boolean renaming = row.name.equals(renamingProfile);
            boolean showDelete = !renaming && ProfileManager.getProfileNames().size() > 1;
            int iconsRight = panelX + panelWidth - PADDING - 8;
            int deleteX = iconsRight - ICON_BOX_SIZE;
            int renameX = deleteX - (showDelete ? ICON_BOX_SIZE + ICON_GAP : 0);
            int iconY = top + (ROW_HEIGHT - ICON_BOX_SIZE) / 2;

            if (mouseY >= iconY && mouseY < iconY + ICON_BOX_SIZE) {
                if (mouseX >= renameX && mouseX < renameX + ICON_BOX_SIZE) {
                    startRename(row.name);
                    return true;
                }
                if (showDelete && mouseX >= deleteX && mouseX < deleteX + ICON_BOX_SIZE) {
                    if (row.name.equals(renamingProfile)) cancelRename();
                    ProfileManager.deleteProfile(row.name);
                    return true;
                }
            }

            if (!renaming) {
                ProfileManager.applyProfile(row.name);
            }
            return true;
        }

        return false;
    }

    private void startRename(String name) {
        cancelRename();
        renamingProfile = name;
        Row row = findRow(name);
        int rowTop = row != null ? row.top - (int) scrollOffset : panelY;

        renameEditBox = new net.minecraft.client.gui.components.EditBox(this.font,
                panelX + PADDING + 8, rowTop + 7, 120, 12, Component.literal("Rename profile"));
        renameEditBox.setBordered(false);
        renameEditBox.setValue(name);
        renameEditBox.setFocused(true);
        this.addRenderableWidget(renameEditBox);
        this.setFocused(renameEditBox);
    }

    private void cancelRename() {
        if (renameEditBox != null) {
            if (this.getFocused() == renameEditBox) {
                this.setFocused(null);
            }
            this.removeWidget(renameEditBox);
        }
        renameEditBox = null;
        renamingProfile = null;
    }

    private void commitRename() {
        if (renamingProfile != null && renameEditBox != null) {
            ProfileManager.renameProfile(renamingProfile, renameEditBox.getValue());
        }
        cancelRename();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (renamingProfile != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                cancelRename();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                commitRename();
                return true;
            }
            return super.keyPressed(event);
        }

        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && newProfileEditBox.isFocused()) {
            String name = newProfileEditBox.getValue();
            if (ProfileManager.createProfile(name)) {
                newProfileEditBox.setValue("");
            }
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= scrollY * ROW_HEIGHT;
        return true;
    }

    private Row findRow(String name) {
        if (name == null) return null;
        for (Row row : computeRows()) {
            if (row.name.equals(name)) return row;
        }
        return null;
    }

    private List<Row> computeRows() {
        List<Row> rows = new ArrayList<>();
        int cursor = panelY + headerHeight();

        for (String name : ProfileManager.getProfileNames()) {
            rows.add(new Row(name, cursor));
            cursor += ROW_HEIGHT;
        }

        return rows;
    }

    private static final class Row {
        final String name;
        final int top;
        final int bottom;

        Row(String name, int top) {
            this.name = name;
            this.top = top;
            this.bottom = top + ROW_HEIGHT;
        }
    }

    // unused
    private interface EditBoxLike {
    }
}