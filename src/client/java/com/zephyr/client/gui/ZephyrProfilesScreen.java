package com.zephyr.client.gui;

import com.zephyr.client.ZephyrConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ZephyrProfilesScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 4;
    private static final int HORIZONTAL_MARGIN = 16;
    private static final int TOP_MARGIN = 16;
    private static final int BOTTOM_MARGIN = 16;
    private static final int TITLE_HEIGHT = 16;
    private static final int TITLE_GAP = 8;
    private static final int INFO_HEIGHT = 12;
    private static final int INFO_GAP = 8;
    private static final int SECTION_GAP = 10;
    private static final int STATUS_HEIGHT = 12;
    private static final int STATUS_GAP = 8;
    private static final int CREATE_BUTTON_WIDTH = 80;
    private static final int DELETE_BUTTON_WIDTH = 80;
    private static final int BACK_BUTTON_WIDTH = 100;
    private static final double SCROLL_STEP = BUTTON_HEIGHT + ROW_GAP;

    private final Screen parent;
    private final List<ScrollableWidget> scrollableWidgets = new ArrayList<>();

    private TextFieldWidget profileNameField;
    private String draftProfileName = "";
    private String statusMessage = "";
    private int statusColor = 0xA0A0A0;
    private int viewportTop;
    private int viewportBottom;
    private int contentHeight;
    private double scrollAmount;
    private double maxScroll;

    public ZephyrProfilesScreen(Screen parent) {
        super(Text.literal("Profiles"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.scrollableWidgets.clear();

        int contentWidth = Math.max(220, this.width - (HORIZONTAL_MARGIN * 2));
        int contentX = (this.width - contentWidth) / 2;
        int createRowY = TOP_MARGIN + TITLE_HEIGHT + TITLE_GAP + INFO_HEIGHT + INFO_GAP;

        int inputWidth = Math.max(120, contentWidth - CREATE_BUTTON_WIDTH - ROW_GAP);
        this.profileNameField = this.addDrawableChild(
                new TextFieldWidget(this.textRenderer, contentX, createRowY, inputWidth, BUTTON_HEIGHT, Text.literal("Profile Name"))
        );
        this.profileNameField.setMaxLength(32);
        this.profileNameField.setText(this.draftProfileName);
        this.profileNameField.setChangedListener(value -> this.draftProfileName = value);

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Create"), button -> this.createProfile())
                        .dimensions(contentX + inputWidth + ROW_GAP, createRowY, CREATE_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );

        int backButtonY = this.height - BOTTOM_MARGIN - BUTTON_HEIGHT;
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Back"), button -> this.close())
                        .dimensions((this.width - BACK_BUTTON_WIDTH) / 2, backButtonY, BACK_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );

        this.viewportTop = createRowY + BUTTON_HEIGHT + SECTION_GAP;
        this.viewportBottom = backButtonY - STATUS_GAP - STATUS_HEIGHT - SECTION_GAP;

        int rowY = 0;
        int selectButtonWidth = Math.max(120, contentWidth - DELETE_BUTTON_WIDTH - ROW_GAP);
        for (ZephyrConfig.ProfileSummary profile : ZephyrConfig.getProfiles()) {
            ButtonWidget selectButton = this.addScrollableChild(
                    ButtonWidget.builder(getProfileLabel(profile), button -> this.selectProfile(profile))
                            .dimensions(contentX, this.viewportTop + rowY, selectButtonWidth, BUTTON_HEIGHT)
                            .build()
            );
            this.registerScrollableWidget(selectButton, contentX, rowY, true);

            ButtonWidget deleteButton = this.addScrollableChild(
                    ButtonWidget.builder(
                                    profile.selected() ? Text.literal("Active") : Text.literal("Delete"),
                                    button -> this.deleteProfile(profile)
                            )
                            .dimensions(contentX + selectButtonWidth + ROW_GAP, this.viewportTop + rowY, DELETE_BUTTON_WIDTH, BUTTON_HEIGHT)
                            .build()
            );
            deleteButton.active = !profile.selected();
            this.registerScrollableWidget(deleteButton, contentX + selectButtonWidth + ROW_GAP, rowY, !profile.selected());
            rowY += BUTTON_HEIGHT + ROW_GAP;
        }

        this.contentHeight = Math.max(0, rowY - ROW_GAP);
        this.maxScroll = Math.max(0.0D, this.contentHeight - Math.max(0, this.viewportBottom - this.viewportTop));
        this.setScrollAmount(this.scrollAmount);
        this.setInitialFocus(this.profileNameField);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return (this.profileNameField != null && this.profileNameField.charTyped(chr, modifiers))
                || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.createProfile();
            return true;
        }

        return (this.profileNameField != null && this.profileNameField.keyPressed(keyCode, scanCode, modifiers))
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.maxScroll <= 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        this.setScrollAmount(this.scrollAmount - (verticalAmount * SCROLL_STEP));
        return true;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, TOP_MARGIN, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Active Profile: " + ZephyrConfig.getSelectedProfileName()),
                this.width / 2,
                TOP_MARGIN + TITLE_HEIGHT + TITLE_GAP,
                0xA0A0A0
        );
        if (!this.statusMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal(this.statusMessage),
                    this.width / 2,
                    this.height - BOTTOM_MARGIN - BUTTON_HEIGHT - STATUS_GAP - STATUS_HEIGHT,
                    this.statusColor
            );
        }
    }

    private void createProfile() {
        try {
            ZephyrConfig.createProfile(this.draftProfileName);
            this.draftProfileName = "";
            this.setStatus("Created profile: " + ZephyrConfig.getSelectedProfileName(), 0x80FF80);
            this.reload();
        } catch (IllegalArgumentException exception) {
            this.setStatus(exception.getMessage(), 0xFF8080);
        }
    }

    private void selectProfile(ZephyrConfig.ProfileSummary profile) {
        try {
            ZephyrConfig.selectProfile(profile.id());
            this.setStatus("Loaded profile: " + profile.name(), 0x80FF80);
            this.reload();
        } catch (IllegalArgumentException exception) {
            this.setStatus(exception.getMessage(), 0xFF8080);
        }
    }

    private void deleteProfile(ZephyrConfig.ProfileSummary profile) {
        try {
            ZephyrConfig.deleteProfile(profile.id());
            this.setStatus("Deleted profile: " + profile.name(), 0x80FF80);
            this.reload();
        } catch (IllegalArgumentException exception) {
            this.setStatus(exception.getMessage(), 0xFF8080);
        }
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
    }

    private void reload() {
        if (this.client == null) {
            return;
        }

        this.clearChildren();
        this.init();
    }

    private void registerScrollableWidget(ClickableWidget widget, int baseX, int baseY, boolean enabledWhenVisible) {
        this.scrollableWidgets.add(new ScrollableWidget(widget, baseX, baseY, enabledWhenVisible));
    }

    private <T extends ClickableWidget> T addScrollableChild(T widget) {
        return this.addDrawableChild(widget);
    }

    private void setScrollAmount(double newScrollAmount) {
        this.scrollAmount = MathHelper.clamp(newScrollAmount, 0.0D, this.maxScroll);
        int scrollOffset = MathHelper.floor(this.scrollAmount);

        for (ScrollableWidget scrollableWidget : this.scrollableWidgets) {
            int y = this.viewportTop + scrollableWidget.baseY - scrollOffset;
            scrollableWidget.widget.setPosition(scrollableWidget.baseX, y);

            boolean visible = y + scrollableWidget.widget.getHeight() > this.viewportTop
                    && y < this.viewportBottom;
            scrollableWidget.widget.visible = visible;
            scrollableWidget.widget.active = visible && scrollableWidget.enabledWhenVisible;
        }
    }

    private Text getProfileLabel(ZephyrConfig.ProfileSummary profile) {
        return profile.selected()
                ? Text.literal(profile.name() + " (Selected)")
                : Text.literal(profile.name());
    }

    private record ScrollableWidget(ClickableWidget widget, int baseX, int baseY, boolean enabledWhenVisible) {
    }
}
