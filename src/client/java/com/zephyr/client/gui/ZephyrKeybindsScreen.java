package com.zephyr.client.gui;

import com.zephyr.client.ZephyrConfig;
import com.zephyr.client.keybind.ZephyrKeybindManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;

public class ZephyrKeybindsScreen extends Screen {
    private static final int BUTTON_MAX_WIDTH = 220;
    private static final int BUTTON_MIN_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int SIDE_PADDING = 16;
    private static final int TOP_PADDING = 40;
    private static final int BOTTOM_PADDING = 36;

    private final Screen parent;
    private final EnumMap<ZephyrKeybindManager.Action, ButtonWidget> actionButtons = new EnumMap<>(ZephyrKeybindManager.Action.class);

    private ButtonWidget modifierButton;
    private CaptureTarget captureTarget;

    public ZephyrKeybindsScreen(Screen parent) {
        super(Text.literal("Keybinds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int availableWidth = Math.max(BUTTON_MIN_WIDTH, this.width - (SIDE_PADDING * 2));
        int columnCount = MathHelper.clamp(
                (availableWidth + BUTTON_GAP) / (BUTTON_MIN_WIDTH + BUTTON_GAP),
                2,
                4
        );
        int buttonWidth = Math.min(
                BUTTON_MAX_WIDTH,
                (availableWidth - ((columnCount - 1) * BUTTON_GAP)) / columnCount
        );
        int contentWidth = (buttonWidth * columnCount) + ((columnCount - 1) * BUTTON_GAP);
        int contentX = (this.width - contentWidth) / 2;
        int currentY = TOP_PADDING;

        this.modifierButton = this.addDrawableChild(ButtonWidget.builder(
                        getModifierLabel(),
                        button -> startCapture(CaptureTarget.modifier())
                )
                .dimensions(contentX, currentY, contentWidth, BUTTON_HEIGHT)
                .build());

        currentY += BUTTON_HEIGHT + BUTTON_GAP + 6;

        ZephyrKeybindManager.Action[] actions = ZephyrKeybindManager.Action.values();

        for (int index = 0; index < actions.length; index++) {
            ZephyrKeybindManager.Action action = actions[index];
            int column = index % columnCount;
            int row = index / columnCount;
            int x = contentX + (column * (buttonWidth + BUTTON_GAP));
            int y = currentY + (row * (BUTTON_HEIGHT + BUTTON_GAP));

            ButtonWidget actionButton = this.addDrawableChild(ButtonWidget.builder(
                            getActionLabel(action),
                            button -> startCapture(CaptureTarget.action(action))
                    )
                    .dimensions(x, y, buttonWidth, BUTTON_HEIGHT)
                    .build());
            actionButtons.put(action, actionButton);
        }

        int actionRows = MathHelper.ceil((float) actions.length / columnCount);
        int backButtonY = currentY + (actionRows * (BUTTON_HEIGHT + BUTTON_GAP)) + 8;

        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Back"),
                        button -> close()
                )
                .dimensions((this.width / 2) - 50, Math.min(backButtonY, this.height - BOTTOM_PADDING), 100, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (handleCapturedKey(keyCode)) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, getHintText(), this.width / 2, this.height - 18, 0xA0A0A0);
    }

    private void startCapture(CaptureTarget newTarget) {
        captureTarget = newTarget;
        refreshLabels();
    }

    private boolean handleCapturedKey(int keyCode) {
        if (captureTarget == null) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            captureTarget = null;
            refreshLabels();
            return true;
        }

        int selectedKey = (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE)
                ? ZephyrKeybindManager.UNBOUND_KEY
                : keyCode;

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
            return Text.literal("Click a keybind, then press a key. Esc closes. Backspace/Delete clears.");
        }

        return Text.literal("Press a key now. Esc cancels. Backspace/Delete clears.");
    }

    private record CaptureTarget(ZephyrKeybindManager.Action action) {
        private static CaptureTarget modifier() {
            return new CaptureTarget(null);
        }

        private static CaptureTarget action(ZephyrKeybindManager.Action action) {
            return new CaptureTarget(action);
        }
    }
}
