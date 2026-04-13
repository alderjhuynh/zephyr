package com.zephyr.client.gui;

import com.zephyr.client.ZephyrConfig;
import com.zephyr.client.keybind.ZephyrKeybindManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class ZephyrKeybindsScreen extends Screen {
    private static final int BUTTON_MAX_WIDTH = 220;
    private static final int BUTTON_MIN_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int HORIZONTAL_MARGIN = 16;
    private static final int TOP_MARGIN = 16;
    private static final int BOTTOM_MARGIN = 16;
    private static final int TITLE_HEIGHT = 16;
    private static final int TITLE_GAP = 8;
    private static final int HINT_HEIGHT = 12;
    private static final int HINT_GAP = 8;
    private static final double SCROLL_STEP = BUTTON_HEIGHT + BUTTON_GAP;

    private final Screen parent;
    private final EnumMap<ZephyrKeybindManager.Action, ButtonWidget> actionButtons = new EnumMap<>(ZephyrKeybindManager.Action.class);
    private final List<ScrollableWidget> scrollableWidgets = new ArrayList<>();

    private ButtonWidget modifierButton;
    private CaptureTarget captureTarget;
    private int viewportTop;
    private int viewportBottom;
    private int contentHeight;
    private double scrollAmount;
    private double maxScroll;

    public ZephyrKeybindsScreen(Screen parent) {
        super(Text.literal("Keybinds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.scrollableWidgets.clear();
        this.actionButtons.clear();

        this.viewportTop = TOP_MARGIN + TITLE_HEIGHT + TITLE_GAP;
        this.viewportBottom = this.height - BOTTOM_MARGIN - HINT_HEIGHT - HINT_GAP;

        int maxContentWidth = Math.max(BUTTON_MIN_WIDTH, this.width - (HORIZONTAL_MARGIN * 2));
        int availableWidth = Math.max(BUTTON_MIN_WIDTH, maxContentWidth);
        int columnCount = MathHelper.clamp(
                (availableWidth + BUTTON_GAP) / (BUTTON_MIN_WIDTH + BUTTON_GAP),
                1,
                4
        );
        int buttonWidth = Math.min(
                BUTTON_MAX_WIDTH,
                (availableWidth - ((columnCount - 1) * BUTTON_GAP)) / columnCount
        );
        int contentWidth = (buttonWidth * columnCount) + ((columnCount - 1) * BUTTON_GAP);
        int contentX = (this.width - contentWidth) / 2;
        int currentY = 0;

        this.modifierButton = this.addScrollableChild(ButtonWidget.builder(
                        getModifierLabel(),
                        button -> startCapture(CaptureTarget.modifier())
                )
                .dimensions(contentX, this.viewportTop, contentWidth, BUTTON_HEIGHT)
                .build());
        this.registerScrollableWidget(this.modifierButton, contentX, currentY);

        currentY += BUTTON_HEIGHT + BUTTON_GAP + 6;

        ZephyrKeybindManager.Action[] actions = ZephyrKeybindManager.Action.values();

        for (int index = 0; index < actions.length; index++) {
            ZephyrKeybindManager.Action action = actions[index];
            int column = index % columnCount;
            int row = index / columnCount;
            int x = contentX + (column * (buttonWidth + BUTTON_GAP));
            int y = currentY + (row * (BUTTON_HEIGHT + BUTTON_GAP));

            ButtonWidget actionButton = this.addScrollableChild(ButtonWidget.builder(
                            getActionLabel(action),
                            button -> startCapture(CaptureTarget.action(action))
                    )
                    .dimensions(x, this.viewportTop + y, buttonWidth, BUTTON_HEIGHT)
                    .build());
            this.registerScrollableWidget(actionButton, x, y);
            actionButtons.put(action, actionButton);
        }

        int actionRows = MathHelper.ceil((float) actions.length / columnCount);
        int backButtonY = currentY + (actionRows * (BUTTON_HEIGHT + BUTTON_GAP)) + 8;
        int backButtonX = (this.width / 2) - 50;

        ButtonWidget backButton = this.addScrollableChild(ButtonWidget.builder(
                        Text.literal("Back"),
                        button -> close()
                )
                .dimensions(backButtonX, this.viewportTop + backButtonY, 100, BUTTON_HEIGHT)
                .build());
        this.registerScrollableWidget(backButton, backButtonX, backButtonY);

        this.contentHeight = backButtonY + BUTTON_HEIGHT;
        this.maxScroll = Math.max(0.0D, this.contentHeight - Math.max(0, this.viewportBottom - this.viewportTop));
        this.setScrollAmount(this.scrollAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (captureTarget == null) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            captureTarget = null;
            refreshLabels();
            return true;
        }

        int selectedKey = keyCode == GLFW.GLFW_KEY_DELETE ? ZephyrKeybindManager.UNBOUND_KEY : keyCode;

        if (captureTarget.action == null) {
            ZephyrKeybindManager.setModifierKeyCode(selectedKey);
        } else {
            ZephyrKeybindManager.setSpecificKeyCode(captureTarget.action, selectedKey);
        }

        ZephyrConfig.saveCurrentState();
        captureTarget = null;
        refreshLabels();
        return true;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, TOP_MARGIN, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, getHintText(), this.width / 2, this.height - BOTTOM_MARGIN - HINT_HEIGHT + 2, 0xA0A0A0);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.maxScroll <= 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        this.setScrollAmount(this.scrollAmount - (verticalAmount * SCROLL_STEP));
        return true;
    }

    private void startCapture(CaptureTarget newTarget) {
        captureTarget = newTarget;
        refreshLabels();
    }

    private void refreshLabels() {
        modifierButton.setMessage(getModifierLabel());

        for (ZephyrKeybindManager.Action action : ZephyrKeybindManager.Action.values()) {
            actionButtons.get(action).setMessage(getActionLabel(action));
        }
    }

    private Text getModifierLabel() {
        if (captureTarget != null && captureTarget.action == null) {
            return Text.literal("Modifier: <press a key>");
        }

        return ZephyrKeybindManager.getModifierButtonText();
    }

    private Text getActionLabel(ZephyrKeybindManager.Action action) {
        if (captureTarget != null && captureTarget.action == action) {
            return Text.literal(action.getDisplayName() + ": <press a key>");
        }

        return ZephyrKeybindManager.getActionButtonText(action);
    }

    private Text getHintText() {
        if (captureTarget == null) {
            return Text.literal("Click a keybind, then press a key. Esc cancels. Delete clears.");
        }

        return Text.literal("Press a key now. Esc cancels. Delete clears.");
    }

    private <T extends ClickableWidget> T addScrollableChild(T widget) {
        return this.addDrawableChild(widget);
    }

    private void registerScrollableWidget(ClickableWidget widget, int baseX, int baseY) {
        this.scrollableWidgets.add(new ScrollableWidget(widget, baseX, baseY));
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
            scrollableWidget.widget.active = visible;
        }
    }

    private record CaptureTarget(ZephyrKeybindManager.Action action) {
        private static CaptureTarget modifier() {
            return new CaptureTarget(null);
        }

        private static CaptureTarget action(ZephyrKeybindManager.Action action) {
            return new CaptureTarget(action);
        }
    }

    private record ScrollableWidget(ClickableWidget widget, int baseX, int baseY) {
    }
}
