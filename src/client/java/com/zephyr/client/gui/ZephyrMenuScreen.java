package com.zephyr.client.gui;

import com.zephyr.client.*;
import com.zephyr.client.disable.*;
import com.zephyr.client.module.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ZephyrMenuScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MAX_WIDTH = 220;
    private static final int BUTTON_MIN_WIDTH = 120;
    private static final int KEYBINDS_BUTTON_WIDTH = 96;
    private static final int WIDGET_GAP = 4;
    private static final int HORIZONTAL_MARGIN = 16;
    private static final int TOP_MARGIN = 16;
    private static final int BOTTOM_MARGIN = 16;
    private static final int TITLE_HEIGHT = 16;
    private static final int HEADER_GAP = 8;
    private static final int VIEWPORT_GAP = 10;
    private static final int SCROLL_HINT_HEIGHT = 0;
    private static final int MIN_SLIDER_WIDTH = 120;
    private static final double SCROLL_STEP = BUTTON_HEIGHT + WIDGET_GAP;
    private final Screen parent;
    private final List<ScrollableWidget> scrollableWidgets = new ArrayList<>();
    private int viewportTop;
    private int viewportBottom;
    private int contentHeight;
    private double scrollAmount;
    private double maxScroll;

    public ZephyrMenuScreen(Screen parent) {
        super(Text.literal("Zephyr"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.scrollableWidgets.clear();
        this.viewportTop = TOP_MARGIN + TITLE_HEIGHT + HEADER_GAP + BUTTON_HEIGHT + VIEWPORT_GAP;
        this.viewportBottom = this.height - BOTTOM_MARGIN - SCROLL_HINT_HEIGHT;

        int headerButtonX = this.width - HORIZONTAL_MARGIN - KEYBINDS_BUTTON_WIDTH;
        int headerButtonY = TOP_MARGIN + TITLE_HEIGHT + HEADER_GAP;
        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Keybinds"),
                                button -> {
                                    if (this.client != null) {
                                        this.client.setScreen(new ZephyrKeybindsScreen(this));
                                    }
                                }
                        )
                        .dimensions(headerButtonX, headerButtonY, KEYBINDS_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Profiles"),
                                button -> {
                                    if (this.client != null) {
                                        this.client.setScreen(new ZephyrProfilesScreen(this));
                                    }
                                }
                        )
                        .dimensions(headerButtonX-KEYBINDS_BUTTON_WIDTH-WIDGET_GAP, headerButtonY, KEYBINDS_BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build()
        );

        MenuButtonSpec[] toggleButtons = new MenuButtonSpec[] {
                new MenuButtonSpec(ZephyrMenuScreen::getAutoRespawnText, () -> AutoRespawn.enabled = !AutoRespawn.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getStepText, () -> Step.setEnabled(!Step.isEnabled())),
                new MenuButtonSpec(ZephyrMenuScreen::getSprintText, () -> Sprint.enabled = !Sprint.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getSneakText, () -> Sneak.enabled = !Sneak.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getAntiHungerText, () -> AntiHunger.enabled = !AntiHunger.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getElytraBoostText, () -> ElytraBoost.enabled = !ElytraBoost.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getElytraSwapText, () -> ElytraSwap.enabled = !ElytraSwap.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getNoFallText, () -> NoFall.enabled = !NoFall.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getItemRestockText, () -> ItemRestock.enabled = !ItemRestock.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getDurabilitySwapText, () -> DurabilitySwap.enabled = !DurabilitySwap.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getHotbarRowSwapText, () -> HotbarRowSwap.enabled = !HotbarRowSwap.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getTridentBoostText, () -> TridentBoost.enabled = !TridentBoost.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getBlinkText, () -> Blink.CanUseKeybind = !Blink.CanUseKeybind),
                new MenuButtonSpec(ZephyrMenuScreen::getAirJumpText, () -> AirJump.setEnabled(!AirJump.enabled)),
                new MenuButtonSpec(ZephyrMenuScreen::getFlightText, () -> Flight.setEnabled(!Flight.enabled)),
                new MenuButtonSpec(ZephyrMenuScreen::getFreeCamText, () -> FreeCam.setEnabled(!FreeCam.enabled)),
                new MenuButtonSpec(ZephyrMenuScreen::getF5TweaksText, () -> F5Tweaks.enabled = !F5Tweaks.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getGuiMoveText, () -> GuiMove.enabled = !GuiMove.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getSpeedMineText, SpeedMine::cycleMode),
                new MenuButtonSpec(ZephyrMenuScreen::getCriticalsText, () -> Criticals.enabled = !Criticals.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getJesusText, () -> Jesus.enabled = !Jesus.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getAutoToolText, () -> AutoTool.enabled = !AutoTool.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getFastAttackText, () -> FastAttack.enabled = !FastAttack.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getShieldBreakerText, () -> ShieldBreaker.enabled = !ShieldBreaker.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getFastUseText, () -> FastUse.enabled = !FastUse.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getGhostHandText, () -> GhostHand.enabled = !GhostHand.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getHoldAttackText, () -> HoldAttack.enabled = !HoldAttack.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getHoldUseText, () -> HoldUse.enabled = !HoldUse.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getPeriodicAttackText, () -> PeriodicAttack.enabled = !PeriodicAttack.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getPeriodicUseText, () -> PeriodicUse.enabled = !PeriodicUse.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getPickBeforePlaceText, () -> PickBeforePlace.enabled = !PickBeforePlace.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getRenderInvisibilityText, () -> RenderInvisibility.enabled = !RenderInvisibility.enabled),
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
                new MenuButtonSpec(ZephyrMenuScreen::getDeadMobRenderingText, () -> disableDeadMobRendering.enabled = !disableDeadMobRendering.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getBreachSwapText, () -> BreachSwap.enabled = !BreachSwap.enabled),
                new MenuButtonSpec(ZephyrMenuScreen::getPearlCatchText, () -> PearlCatch.enabled = !PearlCatch.enabled)
        };

        int availableWidth = Math.max(BUTTON_MIN_WIDTH, this.width - (HORIZONTAL_MARGIN * 2));
        int columnCount = MathHelper.clamp(
                (availableWidth + WIDGET_GAP) / (BUTTON_MIN_WIDTH + WIDGET_GAP),
                1,
                4
        );
        int buttonWidth = Math.min(
                BUTTON_MAX_WIDTH,
                (availableWidth - ((columnCount - 1) * WIDGET_GAP)) / columnCount
        );
        int contentWidth = (buttonWidth * columnCount) + ((columnCount - 1) * WIDGET_GAP);
        int contentX = (this.width - contentWidth) / 2;

        int toggleRowCount = MathHelper.ceil((float) toggleButtons.length / columnCount);
        boolean stackSliderControls = contentWidth < ((BUTTON_MIN_WIDTH * 2) + WIDGET_GAP);
        int sliderRowCount = stackSliderControls ? 16 : 8;
        int rowCount = toggleRowCount + sliderRowCount;

        int viewportHeight = Math.max(BUTTON_HEIGHT, this.viewportBottom - this.viewportTop);
        int gap = MathHelper.clamp((viewportHeight - (rowCount * BUTTON_HEIGHT)) / Math.max(1, rowCount - 1), 2, 6);
        int currentY = 0;

        int index = 0;
        for (MenuButtonSpec spec : toggleButtons) {
            int column = index % columnCount;
            int row = index / columnCount;
            int x = contentX + (column * (buttonWidth + WIDGET_GAP));
            int y = currentY + (row * (BUTTON_HEIGHT + gap));

            this.addMenuButton(
                    spec.textSupplier.get(),
                    b -> applyMenuChange(b, spec.toggleAction, spec.textSupplier),
                    x, y, buttonWidth
            );
            index++;
        }

        currentY = toggleRowCount * (BUTTON_HEIGHT + gap);
        int sliderButtonWidth = stackSliderControls ? contentWidth : Math.max(BUTTON_MIN_WIDTH, Math.min(220, contentWidth / 3));
        int sliderWidth = stackSliderControls ? contentWidth : Math.max(MIN_SLIDER_WIDTH, contentWidth - sliderButtonWidth - WIDGET_GAP);

        currentY = this.addSliderRow(
                contentX,
                currentY,
                contentWidth,
                sliderButtonWidth,
                sliderWidth,
                gap,
                stackSliderControls,
                ButtonWidget.builder(
                                getPearlBoostText(),
                                b -> applyMenuChange(b, () -> PearlBoost.enabled = !PearlBoost.enabled, ZephyrMenuScreen::getPearlBoostText)
                        )
                        .dimensions(contentX, this.viewportTop + currentY, sliderButtonWidth, BUTTON_HEIGHT)
                        .build(),
                new PearlBoostVelocitySlider(contentX, this.viewportTop + currentY, sliderWidth, BUTTON_HEIGHT)
        );
        currentY = this.addSliderRow(
                contentX,
                currentY,
                contentWidth,
                sliderButtonWidth,
                sliderWidth,
                gap,
                stackSliderControls,
                ButtonWidget.builder(
                                getHighJumpText(),
                                b -> applyMenuChange(b, () -> HighJump.enabled = !HighJump.enabled, ZephyrMenuScreen::getHighJumpText)
                        )
                        .dimensions(contentX, this.viewportTop + currentY, sliderButtonWidth, BUTTON_HEIGHT)
                        .build(),
                new HighJumpSlider(contentX, this.viewportTop + currentY, sliderWidth, BUTTON_HEIGHT)
        );
        currentY = this.addSliderRow(
                contentX,
                currentY,
                contentWidth,
                sliderButtonWidth,
                sliderWidth,
                gap,
                stackSliderControls,
                ButtonWidget.builder(
                                getLongJumpText(),
                                b -> applyMenuChange(b, () -> LongJump.setEnabled(!LongJump.enabled), ZephyrMenuScreen::getLongJumpText)
                        )
                        .dimensions(contentX, this.viewportTop + currentY, sliderButtonWidth, BUTTON_HEIGHT)
                        .build(),
                new LongJumpSlider(contentX, this.viewportTop + currentY, sliderWidth, BUTTON_HEIGHT)
        );
        currentY = this.addSliderRow(
                contentX,
                currentY,
                contentWidth,
                sliderButtonWidth,
                sliderWidth,
                gap,
                stackSliderControls,
                ButtonWidget.builder(
                                getAerodynamicsText(),
                                b -> applyMenuChange(b, () -> Aerodynamics.enabled = !Aerodynamics.enabled, ZephyrMenuScreen::getAerodynamicsText)
                        )
                        .dimensions(contentX, this.viewportTop + currentY, sliderButtonWidth, BUTTON_HEIGHT)
                        .build(),
                new AerodynamicsSlider(contentX, this.viewportTop + currentY, sliderWidth, BUTTON_HEIGHT)
        );
        currentY = this.addSliderRow(
                contentX,
                currentY,
                contentWidth,
                sliderButtonWidth,
                sliderWidth,
                gap,
                stackSliderControls,
                ButtonWidget.builder(
                                getFastAttackText(),
                                b -> applyMenuChange(b, () -> FastAttack.enabled = !FastAttack.enabled, ZephyrMenuScreen::getFastAttackText)
                        )
                        .dimensions(contentX, this.viewportTop + currentY, sliderButtonWidth, BUTTON_HEIGHT)
                        .build(),
                new FastAttackSlider(contentX, this.viewportTop + currentY, sliderWidth, BUTTON_HEIGHT)
        );
        currentY = this.addSliderRow(
                contentX,
                currentY,
                contentWidth,
                sliderButtonWidth,
                sliderWidth,
                gap,
                stackSliderControls,
                ButtonWidget.builder(
                                getFastUseText(),
                                b -> applyMenuChange(b, () -> FastUse.enabled = !FastUse.enabled, ZephyrMenuScreen::getFastUseText)
                        )
                        .dimensions(contentX, this.viewportTop + currentY, sliderButtonWidth, BUTTON_HEIGHT)
                        .build(),
                new FastUseSlider(contentX, this.viewportTop + currentY, sliderWidth, BUTTON_HEIGHT)
        );
        currentY = this.addSliderRow(
                contentX,
                currentY,
                contentWidth,
                sliderButtonWidth,
                sliderWidth,
                gap,
                stackSliderControls,
                ButtonWidget.builder(
                                getPeriodicAttackText(),
                                b -> applyMenuChange(b, () -> PeriodicAttack.enabled = !PeriodicAttack.enabled, ZephyrMenuScreen::getPeriodicAttackText)
                        )
                        .dimensions(contentX, this.viewportTop + currentY, sliderButtonWidth, BUTTON_HEIGHT)
                        .build(),
                new PeriodicAttackSlider(contentX, this.viewportTop + currentY, sliderWidth, BUTTON_HEIGHT)
        );
        currentY = this.addSliderRow(
                contentX,
                currentY,
                contentWidth,
                sliderButtonWidth,
                sliderWidth,
                gap,
                stackSliderControls,
                ButtonWidget.builder(
                                getPeriodicUseText(),
                                b -> applyMenuChange(b, () -> PeriodicUse.enabled = !PeriodicUse.enabled, ZephyrMenuScreen::getPeriodicUseText)
                        )
                        .dimensions(contentX, this.viewportTop + currentY, sliderButtonWidth, BUTTON_HEIGHT)
                        .build(),
                new PeriodicUseSlider(contentX, this.viewportTop + currentY, sliderWidth, BUTTON_HEIGHT)
        );

        this.contentHeight = Math.max(0, currentY - gap);
        this.maxScroll = Math.max(0.0D, this.contentHeight - viewportHeight);
        this.setScrollAmount(this.scrollAmount);
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
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.maxScroll <= 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        this.setScrollAmount(this.scrollAmount - (verticalAmount * SCROLL_STEP));
        return true;
    }

    private void addMenuButton(Text text, ButtonWidget.PressAction onPress, int x, int baseY, int width) {
        ButtonWidget button = this.addScrollableChild(
                ButtonWidget.builder(text, onPress).dimensions(x, this.viewportTop + baseY, width, BUTTON_HEIGHT).build()
        );
        this.registerScrollableWidget(button, x, baseY);
    }

    private int addSliderRow(
            int contentX,
            int currentY,
            int contentWidth,
            int sliderButtonWidth,
            int sliderWidth,
            int gap,
            boolean stackSliderControls,
            ButtonWidget button,
            SliderWidget slider
    ) {
        ButtonWidget addedButton = this.addScrollableChild(button);
        this.registerScrollableWidget(addedButton, contentX, currentY);

        if (stackSliderControls) {
            SliderWidget addedSlider = this.addScrollableChild(slider);
            this.registerScrollableWidget(addedSlider, contentX, currentY + BUTTON_HEIGHT + gap);
            return currentY + (2 * (BUTTON_HEIGHT + gap));
        }

        int sliderX = contentX + sliderButtonWidth + WIDGET_GAP;
        slider.setWidth(Math.min(sliderWidth, Math.max(MIN_SLIDER_WIDTH, contentWidth - sliderButtonWidth - WIDGET_GAP)));
        slider.setPosition(sliderX, this.viewportTop + currentY);
        SliderWidget addedSlider = this.addScrollableChild(slider);
        this.registerScrollableWidget(addedSlider, sliderX, currentY);
        return currentY + BUTTON_HEIGHT + gap;
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

    private static Text getSneakText() {
        return Text.literal("Sneak: " + (Sneak.enabled ? "ON" : "OFF"));
    }

    private static Text getElytraBoostText() {
        return Text.literal("Elytra Boost: " + (ElytraBoost.enabled ? "ON" : "OFF"));
    }

    private static Text getElytraSwapText() {
        return Text.literal("Elytra Swap: " + (ElytraSwap.enabled ? "ON" : "OFF"));
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

    private static Text getFastAttackTimesText() {
        return Text.literal("Times/Tick: " + FastAttack.getTimesPerTick());
    }

    private static Text getFastUseTimesText() {
        return Text.literal("Times/Tick: " + FastUse.getTimesPerTick());
    }

    private static Text getPeriodicAttackDelayText() {
        return Text.literal("Delay: " + PeriodicAttack.getDelayTicks() + " ticks");
    }

    private static Text getPeriodicUseDelayText() {
        return Text.literal("Delay: " + PeriodicUse.getDelayTicks() + " ticks");
    }

    private static Text getStepText() {
        return Text.literal("Step: " + (Step.isEnabled() ? "ON" : "OFF"));
    }

    private static Text getDurabilitySwapText() {
        return Text.literal("Durability Swap: " + (DurabilitySwap.enabled ? "ON" : "OFF"));
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

    private static Text getFreeCamText() {
        return Text.literal("FreeCam: " + (FreeCam.enabled ? "ON" : "OFF"));
    }

    private static Text getF5TweaksText() {
        return Text.literal("F5 Tweaks: " + (F5Tweaks.enabled ? "ON" : "OFF"));
    }

    private static Text getGuiMoveText() {
        return Text.literal("GUI Move: " + (GuiMove.enabled ? "ON" : "OFF"));
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

    private static Text getFastAttackText() {
        return Text.literal("Fast Attack: " + (FastAttack.enabled ? "ON" : "OFF"));
    }

    private static Text getShieldBreakerText() {
        return Text.literal("Shield Breaker: " + (ShieldBreaker.enabled ? "ON" : "OFF"));
    }

    private static Text getFastUseText() {
        return Text.literal("Fast Use: " + (FastUse.enabled ? "ON" : "OFF"));
    }

    private static Text getHotbarRowSwapText() {
        return Text.literal("Hotbar Row Swap: " + (HotbarRowSwap.enabled ? "ON" : "OFF"));
    }

    private static Text getGhostHandText() {
        return Text.literal("GhostHand: " + (GhostHand.enabled ? "ON" : "OFF"));
    }

    private static Text getHoldAttackText() {
        return Text.literal("Hold Attack: " + (HoldAttack.enabled ? "ON" : "OFF"));
    }

    private static Text getHoldUseText() {
        return Text.literal("Hold Use: " + (HoldUse.enabled ? "ON" : "OFF"));
    }

    private static Text getPeriodicAttackText() {
        return Text.literal("Periodic Attack: " + (PeriodicAttack.enabled ? "ON" : "OFF"));
    }

    private static Text getPeriodicUseText() {
        return Text.literal("Periodic Use: " + (PeriodicUse.enabled ? "ON" : "OFF"));
    }

    private static Text getPickBeforePlaceText() {
        return Text.literal("Pick Before Place: " + (PickBeforePlace.enabled ? "ON" : "OFF"));
    }

    private static Text getRenderInvisibilityText() {
        return Text.literal("Render Invisibility: " + (RenderInvisibility.enabled ? "ON" : "OFF"));
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

    private static Text getBreachSwapText() {
        return Text.literal("Breach Swap: " + (BreachSwap.enabled ? "ON" : "OFF"));
    }

    private static Text getPearlCatchText() {
        return Text.literal("Pearl Catch: " + (PearlCatch.enabled ? "ON" : "OFF"));
    }

    private static final class MenuButtonSpec {
        private final Supplier<Text> textSupplier;
        private final Runnable toggleAction;

        private MenuButtonSpec(Supplier<Text> textSupplier, Runnable toggleAction) {
            this.textSupplier = textSupplier;
            this.toggleAction = toggleAction;
        }
    }

    private record ScrollableWidget(ClickableWidget widget, int baseX, int baseY) {
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

    private static final class FastAttackSlider extends SliderWidget {
        private FastAttackSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(), toSliderValue(FastAttack.getTimesPerTick()));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(getFastAttackTimesText());
        }

        @Override
        protected void applyValue() {
            FastAttack.setTimesPerTick(fromSliderValue(this.value));
            this.value = toSliderValue(FastAttack.getTimesPerTick());
            this.updateMessage();
        }

        private static double toSliderValue(int value) {
            double min = FastAttack.getMinTimesPerTick();
            double max = FastAttack.getMaxTimesPerTick();
            return (value - min) / (max - min);
        }

        private static int fromSliderValue(double sliderValue) {
            double min = FastAttack.getMinTimesPerTick();
            double max = FastAttack.getMaxTimesPerTick();
            return (int) Math.round(min + ((max - min) * sliderValue));
        }
    }

    private static final class FastUseSlider extends SliderWidget {
        private FastUseSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(), toSliderValue(FastUse.getTimesPerTick()));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(getFastUseTimesText());
        }

        @Override
        protected void applyValue() {
            FastUse.setTimesPerTick(fromSliderValue(this.value));
            this.value = toSliderValue(FastUse.getTimesPerTick());
            this.updateMessage();
        }

        private static double toSliderValue(int value) {
            double min = FastUse.getMinTimesPerTick();
            double max = FastUse.getMaxTimesPerTick();
            return (value - min) / (max - min);
        }

        private static int fromSliderValue(double sliderValue) {
            double min = FastUse.getMinTimesPerTick();
            double max = FastUse.getMaxTimesPerTick();
            return (int) Math.round(min + ((max - min) * sliderValue));
        }
    }

    private static final class PeriodicAttackSlider extends SliderWidget {
        private PeriodicAttackSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(), toSliderValue(PeriodicAttack.getDelayTicks()));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(getPeriodicAttackDelayText());
        }

        @Override
        protected void applyValue() {
            PeriodicAttack.setDelayTicks(fromSliderValue(this.value));
            this.value = toSliderValue(PeriodicAttack.getDelayTicks());
            this.updateMessage();
        }

        private static double toSliderValue(int value) {
            double min = PeriodicAttack.getMinDelayTicks();
            double max = PeriodicAttack.getMaxDelayTicks();
            return (value - min) / (max - min);
        }

        private static int fromSliderValue(double sliderValue) {
            double min = PeriodicAttack.getMinDelayTicks();
            double max = PeriodicAttack.getMaxDelayTicks();
            return (int) Math.round(min + ((max - min) * sliderValue));
        }
    }

    private static final class PeriodicUseSlider extends SliderWidget {
        private PeriodicUseSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(), toSliderValue(PeriodicUse.getDelayTicks()));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(getPeriodicUseDelayText());
        }

        @Override
        protected void applyValue() {
            PeriodicUse.setDelayTicks(fromSliderValue(this.value));
            this.value = toSliderValue(PeriodicUse.getDelayTicks());
            this.updateMessage();
        }

        private static double toSliderValue(int value) {
            double min = PeriodicUse.getMinDelayTicks();
            double max = PeriodicUse.getMaxDelayTicks();
            return (value - min) / (max - min);
        }

        private static int fromSliderValue(double sliderValue) {
            double min = PeriodicUse.getMinDelayTicks();
            double max = PeriodicUse.getMaxDelayTicks();
            return (int) Math.round(min + ((max - min) * sliderValue));
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
