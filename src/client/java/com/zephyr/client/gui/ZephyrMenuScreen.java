package com.zephyr.client.gui;

import com.zephyr.client.*;
import com.zephyr.client.disable.*;
import com.zephyr.client.module.*;
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
    private static final int BUTTON_MIN_WIDTH = 120;
    private static final int KEYBINDS_BUTTON_WIDTH = 96;
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
        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Keybinds"),
                                button -> {
                                    if (this.client != null) {
                                        this.client.setScreen(new ZephyrKeybindsScreen(this));
                                    }
                                }
                        )
                        .dimensions(this.width - KEYBINDS_BUTTON_WIDTH - 10, 10, KEYBINDS_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );

        MenuButtonSpec[] toggleButtons = new MenuButtonSpec[] {
                new MenuButtonSpec(ZephyrMenuScreen::getAutoRespawnText, () -> AutoRespawn.enabled = !AutoRespawn.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getStepText, () -> Step.setEnabled(!Step.isEnabled())),
                new MenuButtonSpec(ZephyrMenuScreen::getSprintText, () -> Sprint.enabled = !Sprint.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getAntiHungerText, () -> AntiHunger.enabled = !AntiHunger.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getElytraBoostText, () -> ElytraBoost.enabled = !ElytraBoost.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getNoFallText, () -> NoFall.enabled = !NoFall.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getItemRestockText, () -> ItemRestock.enabled = !ItemRestock.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getTridentBoostText, () -> TridentBoost.enabled = !TridentBoost.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getBlinkText, () -> Blink.CanUseKeybind = !Blink.CanUseKeybind),
                new MenuButtonSpec(ZephyrMenuScreen::getAirJumpText, () -> AirJump.setEnabled(!AirJump.enabled)),
                new MenuButtonSpec(ZephyrMenuScreen::getFlightText, () -> Flight.setEnabled(!Flight.enabled)),
                new MenuButtonSpec(ZephyrMenuScreen::getSpeedMineText, SpeedMine::cycleMode),
                new MenuButtonSpec(ZephyrMenuScreen::getCriticalsText, () -> Criticals.enabled = !Criticals.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getJesusText, () -> Jesus.enabled = !Jesus.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getAutoToolText, () -> AutoTool.enabled = !AutoTool.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getGhostHandText, () -> GhostHand.enabled = !GhostHand.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getPortalGuiClosingText, () -> disablePortalGuiClosing.enabled = !disablePortalGuiClosing.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getDeadMobInteractionText, () -> disableDeadMobInteraction.enabled = !disableDeadMobInteraction.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getAxeStrippingText, () -> disableAxeStripping.enabled = !disableAxeStripping.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getShovelPathingText, () -> disableShovelPathing.enabled = !disableShovelPathing.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getBreakCooldownText, () -> disableBlockBreakingCooldown.enabled = !disableBlockBreakingCooldown.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getBreakParticlesText, () -> disableBlockBreakingParticles.enabled = !disableBlockBreakingParticles.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getInventoryEffectsText, () -> disableInventoryEffectRendering.enabled = !disableInventoryEffectRendering.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getNauseaOverlayText, () -> disableNauseaOverlay.enabled = !disableNauseaOverlay.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getPortalSoundText, () -> disableNetherPortalSound.enabled = !disableNetherPortalSound.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getFogText, () -> disableFogRendering.enabled = !disableFogRendering.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getFirstPersonParticlesText, () -> disableFirstPersonEffectParticles.enabled = !disableFirstPersonEffectParticles.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getRainText, () -> disableRainEffects.enabled = !disableRainEffects.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getDeadMobRenderingText, () -> disableDeadMobRendering.enabled = !disableDeadMobRendering.enabled)
        };

        int availableWidth = Math.max(BUTTON_MIN_WIDTH, this.width - (SIDE_PADDING * 2));
        int columnCount = MathHelper.clamp(
                (availableWidth + WIDGET_GAP) / (BUTTON_MIN_WIDTH + WIDGET_GAP),
                2,
                4
        );
        int buttonWidth = Math.min(
                BUTTON_MAX_WIDTH,
                (availableWidth - ((columnCount - 1) * WIDGET_GAP)) / columnCount
        );
        int contentWidth = (buttonWidth * columnCount) + ((columnCount - 1) * WIDGET_GAP);
        int contentX = (this.width - contentWidth) / 2;

        int toggleRowCount = MathHelper.ceil((float) toggleButtons.length / columnCount);
        int sliderRowCount = 4;
        int rowCount = toggleRowCount + sliderRowCount;

        int gap = MathHelper.clamp(
                (this.height - (TOP_BOTTOM_PADDING * 2) - (rowCount * BUTTON_HEIGHT)) / Math.max(1, rowCount - 1),
                2,
                6
        );

        int totalHeight = (rowCount * BUTTON_HEIGHT) + ((rowCount - 1) * gap);
        int startY = Math.max(TOP_BOTTOM_PADDING, ((this.height - totalHeight) / 2) - LAYOUT_SHIFT_Y);

        int index = 0;
        for (MenuButtonSpec spec : toggleButtons) {
            int column = index % columnCount;
            int row = index / columnCount;
            int x = contentX + (column * (buttonWidth + WIDGET_GAP));
            int y = startY + (row * (BUTTON_HEIGHT + gap));

            this.addMenuButton(
                    spec.textSupplier.get(),
                    b -> applyMenuChange(b, spec.toggleAction, spec.textSupplier),
                    x, y, buttonWidth, gap
            );
            index++;
        }

        int sliderRowY = startY + (toggleRowCount * (BUTTON_HEIGHT + gap));
        int sliderRowX = contentX;
        int sliderButtonWidth = Math.max(120, Math.min(220, contentWidth / 3));
        int sliderWidth = contentWidth - sliderButtonWidth - WIDGET_GAP;

        this.addDrawableChild(
                ButtonWidget.builder(
                                getPearlBoostText(),
                                b -> applyMenuChange(b, () -> PearlBoost.enabled = !PearlBoost.enabled, ZephyrMenuScreen::getPearlBoostText)
                        )
                        .dimensions(sliderRowX, sliderRowY, sliderButtonWidth, BUTTON_HEIGHT)
                        .build()
        );
        this.addDrawableChild(new PearlBoostVelocitySlider(
                sliderRowX + sliderButtonWidth + WIDGET_GAP,
                sliderRowY,
                sliderWidth,
                BUTTON_HEIGHT
        ));

        int highJumpRowY = sliderRowY + BUTTON_HEIGHT + gap;

        this.addDrawableChild(
                ButtonWidget.builder(
                                getHighJumpText(),
                                b -> applyMenuChange(b, () -> HighJump.enabled = !HighJump.enabled, ZephyrMenuScreen::getHighJumpText)
                        )
                        .dimensions(sliderRowX, highJumpRowY, sliderButtonWidth, BUTTON_HEIGHT)
                        .build()
        );

        this.addDrawableChild(new HighJumpSlider(
                sliderRowX + sliderButtonWidth + WIDGET_GAP,
                highJumpRowY,
                sliderWidth,
                BUTTON_HEIGHT
        ));

        int longJumpRowY = highJumpRowY + BUTTON_HEIGHT + gap;

        this.addDrawableChild(
                ButtonWidget.builder(
                                getLongJumpText(),
                                b -> applyMenuChange(b, () -> LongJump.setEnabled(!LongJump.enabled), ZephyrMenuScreen::getLongJumpText)
                        )
                        .dimensions(sliderRowX, longJumpRowY, sliderButtonWidth, BUTTON_HEIGHT)
                        .build()
        );

        this.addDrawableChild(new LongJumpSlider(
                sliderRowX + sliderButtonWidth + WIDGET_GAP,
                longJumpRowY,
                sliderWidth,
                BUTTON_HEIGHT
        ));

        int aerodynamicsRowY = longJumpRowY + BUTTON_HEIGHT + gap;

        this.addDrawableChild(
                ButtonWidget.builder(
                                getAerodynamicsText(),
                                b -> applyMenuChange(b, () -> Aerodynamics.enabled = !Aerodynamics.enabled, ZephyrMenuScreen::getAerodynamicsText)
                        )
                        .dimensions(sliderRowX, aerodynamicsRowY, sliderButtonWidth, BUTTON_HEIGHT)
                        .build()
        );

        this.addDrawableChild(new AerodynamicsSlider(
                sliderRowX + sliderButtonWidth + WIDGET_GAP,
                aerodynamicsRowY,
                sliderWidth,
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

    private static Text getAutoToolText() {
        return Text.literal("AutoTool: " + (AutoTool.enabled ? "ON" : "OFF"));
    }

    private static Text getGhostHandText() {
        return Text.literal("GhostHand: " + (GhostHand.enabled ? "ON" : "OFF"));
    }

    private static Text getPortalGuiClosingText() {
        return Text.literal("Disable Portal GUI Closing: " + (disablePortalGuiClosing.enabled ? "ON" : "OFF"));
    }

    private static Text getDeadMobInteractionText() {
        return Text.literal("Disable Dead Mob Interaction: " + (disableDeadMobInteraction.enabled ? "ON" : "OFF"));
    }

    private static Text getAxeStrippingText() {
        return Text.literal("Disable Axe Stripping: " + (disableAxeStripping.enabled ? "ON" : "OFF"));
    }

    private static Text getShovelPathingText() {
        return Text.literal("Disable Shovel Pathing: " + (disableShovelPathing.enabled ? "ON" : "OFF"));
    }


    private static Text getBreakCooldownText() {
        return Text.literal("Disable Block Break Cooldown: " + (disableBlockBreakingCooldown.enabled ? "ON" : "OFF"));
    }

    private static Text getBreakParticlesText() {
        return Text.literal("Disable Block Break Particles: " + (disableBlockBreakingParticles.enabled ? "ON" : "OFF"));
    }

    private static Text getInventoryEffectsText() {
        return Text.literal("Disable Inventory Effect Rendering: " + (disableInventoryEffectRendering.enabled ? "ON" : "OFF"));
    }

    private static Text getNauseaOverlayText() {
        return Text.literal("Disable Nausea Overlay: " + (disableNauseaOverlay.enabled ? "ON" : "OFF"));
    }

    private static Text getPortalSoundText() {
        return Text.literal("Disable Portal Sound: " + (disableNetherPortalSound.enabled ? "ON" : "OFF"));
    }

    private static Text getFogText() {
        return Text.literal("Disable Fog: " + (disableFogRendering.enabled ? "ON" : "OFF"));
    }

    private static Text getFirstPersonParticlesText() {
        return Text.literal("Disable First Person Effect Particles: " + (disableFirstPersonEffectParticles.enabled ? "ON" : "OFF"));
    }

    private static Text getRainText() {
        return Text.literal("Disable Rain Effects: " + (disableRainEffects.enabled ? "ON" : "OFF"));
    }

    private static Text getDeadMobRenderingText() {
        return Text.literal("Disable Dead Mob Rendering: " + (disableDeadMobRendering.enabled ? "ON" : "OFF"));
    }

    private static final class MenuButtonSpec {
        private final Supplier<Text> textSupplier;
        private final Runnable toggleAction;

        private MenuButtonSpec(Supplier<Text> textSupplier, Runnable toggleAction) {
            this.textSupplier = textSupplier;
            this.toggleAction = toggleAction;
        }
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
