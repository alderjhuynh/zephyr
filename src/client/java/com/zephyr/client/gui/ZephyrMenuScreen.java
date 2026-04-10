package com.zephyr.client.gui;

import com.zephyr.client.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.Supplier;

public class ZephyrMenuScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MAX_WIDTH = 220;
    private static final int WIDGET_GAP = 4;
    private static final int SIDE_PADDING = 16;
    private static final int TOP_BOTTOM_PADDING = 16;
    private static final int LAYOUT_SHIFT_Y = 24*2;
    private final Screen parent;

    public ZephyrMenuScreen(Screen parent) {
        super(Text.literal("Zephyr"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int leftButtonCount = 7;
        int rightButtonCount = 7;

        int fullWidth = Math.max(80, Math.min(BUTTON_MAX_WIDTH, this.width - (SIDE_PADDING * 2)));
        int buttonWidth = (fullWidth / 2) - (WIDGET_GAP / 2);

        int leftX = (this.width / 2) - buttonWidth - (WIDGET_GAP / 2);
        int rightX = (this.width / 2) + (WIDGET_GAP / 2);

        int rowCount = Math.max(leftButtonCount, rightButtonCount);

        int gap = MathHelper.clamp(
                (this.height - (TOP_BOTTOM_PADDING * 2) - (rowCount * BUTTON_HEIGHT)) / Math.max(1, rowCount - 1),
                2,
                6
        );

        int totalHeight = (rowCount * BUTTON_HEIGHT) + ((rowCount - 1) * gap);
        int startY = Math.max(TOP_BOTTOM_PADDING, ((this.height - totalHeight) / 2) - LAYOUT_SHIFT_Y);

        int yLeft = startY;
        int yRight = startY;

        yLeft = this.addMenuButton(
                getAutoRespawnText(),
                b -> applyMenuChange(b, () -> AutoRespawn.enabled = !AutoRespawn.enabled, ZephyrMenuScreen::getAutoRespawnText),
                leftX, yLeft, buttonWidth, gap
        );

        yLeft = this.addMenuButton(
                getStepText(),
                b -> applyMenuChange(b, () -> Step.setEnabled(!Step.isEnabled()), ZephyrMenuScreen::getStepText),
                leftX, yLeft, buttonWidth, gap
        );

        yLeft = this.addMenuButton(
                getSprintText(),
                b -> applyMenuChange(b, () -> Sprint.enabled = !Sprint.enabled, ZephyrMenuScreen::getSprintText),
                leftX, yLeft, buttonWidth, gap
        );

        yLeft = this.addMenuButton(
                getAntiHungerText(),
                b -> applyMenuChange(b, () -> AntiHunger.enabled = !AntiHunger.enabled, ZephyrMenuScreen::getAntiHungerText),
                leftX, yLeft, buttonWidth, gap
        );

        yLeft = this.addMenuButton(
                getElytraBoostText(),
                b -> applyMenuChange(b, () -> ElytraBoost.enabled = !ElytraBoost.enabled, ZephyrMenuScreen::getElytraBoostText),
                leftX, yLeft, buttonWidth, gap
        );

        yLeft = this.addMenuButton(
                getNoFallText(),
                b -> applyMenuChange(b, () -> NoFall.enabled = !NoFall.enabled, ZephyrMenuScreen::getNoFallText),
                leftX, yLeft, buttonWidth, gap
        );

        yLeft = this.addMenuButton(
                getItemRestockText(),
                b -> applyMenuChange(b, () -> ItemRestock.enabled = !ItemRestock.enabled, ZephyrMenuScreen::getItemRestockText),
                leftX, yLeft, buttonWidth, gap
        );

        yRight = this.addMenuButton(
                getTridentBoostText(),
                b -> applyMenuChange(b, () -> TridentBoost.enabled = !TridentBoost.enabled, ZephyrMenuScreen::getTridentBoostText),
                rightX, yRight, buttonWidth, gap
        );

        yRight = this.addMenuButton(
                getBlinkText(),
                b -> applyMenuChange(b, () -> Blink.CanUseKeybind = !Blink.CanUseKeybind, ZephyrMenuScreen::getBlinkText),
                rightX, yRight, buttonWidth, gap
        );

        yRight = this.addMenuButton(
                getAirJumpText(),
                b -> applyMenuChange(b, () -> AirJump.setEnabled(!AirJump.enabled), ZephyrMenuScreen::getAirJumpText),
                rightX, yRight, buttonWidth, gap
        );

        yRight = this.addMenuButton(
                getFlightText(),
                b -> applyMenuChange(b, () -> Flight.setEnabled(!Flight.enabled), ZephyrMenuScreen::getFlightText),
                rightX, yRight, buttonWidth, gap
        );

        yRight = this.addMenuButton(
                getSpeedMineText(),
                b -> applyMenuChange(b, SpeedMine::cycleMode, ZephyrMenuScreen::getSpeedMineText),
                rightX, yRight, buttonWidth, gap
        );

        yRight = this.addMenuButton(
                getCriticalsText(),
                b -> applyMenuChange(b, () -> Criticals.enabled = !Criticals.enabled, ZephyrMenuScreen::getCriticalsText),
                rightX, yRight, buttonWidth, gap
        );

        yRight = this.addMenuButton(
                getJesusText(),
                b -> applyMenuChange(b, () -> Jesus.enabled = !Jesus.enabled, ZephyrMenuScreen::getJesusText),
                rightX, yRight, buttonWidth, gap
        );

        int pearlRowY = Math.max(yLeft, yRight);
        int pearlRowX = (this.width / 2) - (fullWidth / 2);
        int pearlButtonWidth = Math.max(88, Math.min(buttonWidth, fullWidth / 2));
        int pearlSliderWidth = fullWidth - pearlButtonWidth - WIDGET_GAP;

        this.addDrawableChild(
                ButtonWidget.builder(
                                getPearlBoostText(),
                                b -> applyMenuChange(b, () -> PearlBoost.enabled = !PearlBoost.enabled, ZephyrMenuScreen::getPearlBoostText)
                        )
                        .dimensions(pearlRowX, pearlRowY, pearlButtonWidth, BUTTON_HEIGHT)
                        .build()
        );
        this.addDrawableChild(new PearlBoostVelocitySlider(
                pearlRowX + pearlButtonWidth + WIDGET_GAP,
                pearlRowY,
                pearlSliderWidth,
                BUTTON_HEIGHT
        ));

        int highJumpRowY = pearlRowY + BUTTON_HEIGHT + gap;
        int highJumpRowX = (this.width / 2) - (fullWidth / 2);
        int highJumpButtonWidth = Math.max(88, Math.min(buttonWidth, fullWidth / 2));
        int highJumpSliderWidth = fullWidth - highJumpButtonWidth - WIDGET_GAP;

        this.addDrawableChild(
                ButtonWidget.builder(
                                getHighJumpText(),
                                b -> applyMenuChange(b, () -> HighJump.enabled = !HighJump.enabled, ZephyrMenuScreen::getHighJumpText)
                        )
                        .dimensions(highJumpRowX, highJumpRowY, highJumpButtonWidth, BUTTON_HEIGHT)
                        .build()
        );

        this.addDrawableChild(new HighJumpSlider(
                highJumpRowX + highJumpButtonWidth + WIDGET_GAP,
                highJumpRowY,
                highJumpSliderWidth,
                BUTTON_HEIGHT
        ));

        int longJumpRowY = highJumpRowY + BUTTON_HEIGHT + gap;
        int longJumpRowX = (this.width / 2) - (fullWidth / 2);
        int longJumpButtonWidth = Math.max(88, Math.min(buttonWidth, fullWidth / 2));
        int longJumpSliderWidth = fullWidth - longJumpButtonWidth - WIDGET_GAP;

        this.addDrawableChild(
                ButtonWidget.builder(
                                getLongJumpText(),
                                b -> applyMenuChange(b, () -> LongJump.setEnabled(!LongJump.enabled), ZephyrMenuScreen::getLongJumpText)
                        )
                        .dimensions(longJumpRowX, longJumpRowY, longJumpButtonWidth, BUTTON_HEIGHT)
                        .build()
        );

        this.addDrawableChild(new LongJumpSlider(
                longJumpRowX + longJumpButtonWidth + WIDGET_GAP,
                longJumpRowY,
                longJumpSliderWidth,
                BUTTON_HEIGHT
        ));

        int aerodynamicsRowY = longJumpRowY + BUTTON_HEIGHT + gap;
        int aerodynamicsRowX = (this.width / 2) - (fullWidth / 2);
        int aerodynamicsButtonWidth = Math.max(88, Math.min(buttonWidth, fullWidth / 2));
        int aerodynamicsSliderWidth = fullWidth - aerodynamicsButtonWidth - WIDGET_GAP;

        this.addDrawableChild(
                ButtonWidget.builder(
                                getAerodynamicsText(),
                                b -> applyMenuChange(b, () -> Aerodynamics.enabled = !Aerodynamics.enabled, ZephyrMenuScreen::getAerodynamicsText)
                        )
                        .dimensions(aerodynamicsRowX, aerodynamicsRowY, aerodynamicsButtonWidth, BUTTON_HEIGHT)
                        .build()
        );

        this.addDrawableChild(new AerodynamicsSlider(
                aerodynamicsRowX + aerodynamicsButtonWidth + WIDGET_GAP,
                aerodynamicsRowY,
                aerodynamicsSliderWidth,
                BUTTON_HEIGHT
        ));
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
    }

    private int addMenuButton(Text text, ButtonWidget.PressAction onPress, int x, int y, int width, int gap) {
        this.addDrawableChild(ButtonWidget.builder(text, onPress).dimensions(x, y, width, BUTTON_HEIGHT).build());
        return y + BUTTON_HEIGHT + gap;
    }

    private static void applyMenuChange(ButtonWidget button, Runnable change, Supplier<Text> textSupplier) {
        change.run();
        ZephyrConfig.saveCurrentState();
        button.setMessage(textSupplier.get());
    }

    private static Text getSprintText() {
        return Text.literal("Sprint: " + (Sprint.enabled ? "ON" : "OFF"));
    }

    private static Text getAntiHungerText() {
        return Text.literal("AntiHunger: " + (AntiHunger.enabled ? "ON" : "OFF"));
    }

    private static Text getElytraBoostText() {
        return Text.literal("Elytra Boost: " + (ElytraBoost.enabled ? "ON" : "OFF"));
    }

    private static Text getNoFallText() {
        return Text.literal("No Fall: " + (NoFall.enabled ? "ON" : "OFF"));
    }

    private static Text getCriticalsText() {
        return Text.literal("Criticals: " + (Criticals.enabled ? "ON" : "OFF"));
    }

    private static Text getJesusText() {
        return Text.literal("Jesus: " + (Jesus.enabled ? "ON" : "OFF"));
    }

    private static Text getTridentBoostText() {
        return Text.literal("Trident Boost: " + (TridentBoost.enabled ? "ON" : "OFF"));
    }

    private static Text getBlinkText() {return Text.literal("Blink: " + (Blink.CanUseKeybind ? "ON" : "OFF"));}

    private static Text getPearlBoostText() {
        return Text.literal("Pearl Boost: " + (PearlBoost.enabled ? "ON" : "OFF"));
    }

    private static Text getHighJumpText() {
        return Text.literal("High Jump: " + (HighJump.enabled ? "ON" : "OFF"));
    }

    private static Text getLongJumpText() {
        return Text.literal("Long Jump: " + (LongJump.enabled ? "ON" : "OFF"));
    }

    private static Text getAerodynamicsText() {
        return Text.literal("Aerodynamics: " + (Aerodynamics.enabled ? "ON" : "OFF"));
    }

    private static Text getPearlBoostVelocityText() {
        return Text.literal(String.format("Velocity: %.1f", PearlBoost.getBoostVelocity()));
    }

    private static Text getStepText() {
        return Text.literal("Step: " + (Step.isEnabled() ? "ON" : "OFF"));
    }

    private static Text getAutoRespawnText() {
        return Text.literal("AutoRespawn: " + (AutoRespawn.enabled() ? "ON" : "OFF"));
    }

    private static Text getAirJumpText() {
        return Text.literal("AirJump: " + (AirJump.enabled ? "ON" : "OFF"));
    }

    private static Text getFlightText() {
        return Text.literal("Flight: " + (Flight.enabled ? "ON" : "OFF"));
    }

    private static Text getSpeedMineText() {
        return Text.literal("SpeedMine: " + SpeedMine.mode.name());
    }

    private static Text getItemRestockText() {
        return Text.literal("Item Restock: " + (ItemRestock.enabled ? "ON" : "OFF"));
    }

    public class HighJumpSlider extends SliderWidget {

        private static final double STEP = 0.1D;

        public HighJumpSlider(int x, int y, int width, int height) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Text.empty(),
                    toSliderValue(HighJump.getMultiplier())
            );
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal(String.format("Multiplier: %.2f", HighJump.getMultiplier())));
        }

        @Override
        protected void applyValue() {
            double value = fromSliderValue(this.value);
            HighJump.setMultiplier(value);

            this.value = toSliderValue(HighJump.getMultiplier());
            this.updateMessage();
        }

        private static double toSliderValue(double multiplier) {
            double min = HighJump.getMinMultiplier();
            double max = HighJump.getMaxMultiplier();
            return (multiplier - min) / (max - min);
        }

        private static double fromSliderValue(double sliderValue) {
            double min = HighJump.getMinMultiplier();
            double max = HighJump.getMaxMultiplier();

            double raw = min + ((max - min) * sliderValue);
            return Math.round(raw / STEP) * STEP;
        }
    }

    public class LongJumpSlider extends SliderWidget {

        private static final double STEP = 0.1D;

        public LongJumpSlider(int x, int y, int width, int height) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Text.empty(),
                    toSliderValue(LongJump.getMomentum())
            );
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal(String.format("Momentum: %.2f", LongJump.getMomentum())));
        }

        @Override
        protected void applyValue() {
            double value = fromSliderValue(this.value);
            LongJump.setMomentum(value);

            this.value = toSliderValue(LongJump.getMomentum());
            this.updateMessage();
        }

        private static double toSliderValue(double momentum) {
            double min = LongJump.getMinMomentum();
            double max = LongJump.getMaxMomentum();
            return (momentum - min) / (max - min);
        }

        private static double fromSliderValue(double sliderValue) {
            double min = LongJump.getMinMomentum();
            double max = LongJump.getMaxMomentum();

            double raw = min + ((max - min) * sliderValue);
            return Math.round(raw / STEP) * STEP;
        }
    }

    public class AerodynamicsSlider extends SliderWidget {

        private static final double STEP = 0.005D;

        public AerodynamicsSlider(int x, int y, int width, int height) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Text.empty(),
                    toSliderValue(Aerodynamics.getAcceleration())
            );
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal(String.format("Acceleration: %.3f", Aerodynamics.getAcceleration())));
        }

        @Override
        protected void applyValue() {
            double value = fromSliderValue(this.value);
            Aerodynamics.setAcceleration(value);

            this.value = toSliderValue(Aerodynamics.getAcceleration());
            this.updateMessage();
        }

        private static double toSliderValue(double acceleration) {
            double min = Aerodynamics.getMinAcceleration();
            double max = Aerodynamics.getMaxAcceleration();
            return (acceleration - min) / (max - min);
        }

        private static double fromSliderValue(double sliderValue) {
            double min = Aerodynamics.getMinAcceleration();
            double max = Aerodynamics.getMaxAcceleration();

            double raw = min + ((max - min) * sliderValue);
            return Math.round(raw / STEP) * STEP;
        }
    }

    private static final class PearlBoostVelocitySlider extends SliderWidget {
        private static final double STEP = 0.1D;

        private PearlBoostVelocitySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(), toSliderValue(PearlBoost.getBoostVelocity()));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(getPearlBoostVelocityText());
        }

        @Override
        protected void applyValue() {
            PearlBoost.setBoostVelocity(fromSliderValue(this.value));
            this.value = toSliderValue(PearlBoost.getBoostVelocity());
            this.updateMessage();
        }

        private static double toSliderValue(double velocity) {
            double min = PearlBoost.getMinBoostVelocity();
            double max = PearlBoost.getMaxBoostVelocity();
            return (velocity - min) / (max - min);
        }

        private static double fromSliderValue(double sliderValue) {
            double min = PearlBoost.getMinBoostVelocity();
            double max = PearlBoost.getMaxBoostVelocity();
            double velocity = min + ((max - min) * sliderValue);
            return Math.round(velocity / STEP) * STEP;
        }
    }
}
