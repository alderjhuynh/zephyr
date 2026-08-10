package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.module.qol.appleskin.FoodHelper;
import com.zephyr.client.module.qol.appleskin.HudOverlayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/**
 * Port of AppleSkin's food-related HUD improvements. While enabled it draws
 * overlays on the vanilla HUD: current saturation on the food bar, an
 * exhaustion bar underneath, and, while holding food, the hunger/saturation it
 * would restore plus the health that food would eventually regen.
 *
 * <p>Rendering hooks live in {@code com.zephyr.client.mixin.qol.AppleSkin.HudMixin}
 * ({@code Hud.extractFood} / {@code Hud.extractPlayerHealth}).
 */
public final class AppleSkin extends Module {
    public static final AppleSkin INSTANCE = new AppleSkin();

    private final BooleanSetting showSaturationHudOverlay = new BooleanSetting("Saturation Overlay", true);
    private final BooleanSetting showFoodExhaustionHudUnderlay = new BooleanSetting("Exhaustion Underlay", true);
    private final BooleanSetting showFoodValuesHudOverlay = new BooleanSetting("Food Values Overlay", true);
    private final BooleanSetting showFoodHealthHudOverlay = new BooleanSetting("Health Values Overlay", true);
    private final BooleanSetting showFoodValuesHudOverlayWhenOffhand = new BooleanSetting("Check Offhand", false);
    private final BooleanSetting showVanillaAnimationsOverlay = new BooleanSetting("Match Vanilla Animations", true);
    private final NumberSetting maxHudOverlayFlashAlpha = new NumberSetting("Overlay Flash Alpha", 1.0, 0.0, 1.0, 0.05);

    private final HudOverlayHandler overlay = new HudOverlayHandler();

    private float unclampedFlashAlpha = 0f;
    private float flashAlpha = 0f;
    private byte alphaDir = 1;

    private AppleSkin() {
        super("AppleSkin", "Shows hunger, saturation, exhaustion, and estimated health restored on the HUD", Category.QOL);
        addSetting(showSaturationHudOverlay);
        addSetting(showFoodExhaustionHudUnderlay);
        addSetting(showFoodValuesHudOverlay);
        addSetting(showFoodHealthHudOverlay);
        addSetting(showFoodValuesHudOverlayWhenOffhand);
        addSetting(showVanillaAnimationsOverlay);
        addSetting(maxHudOverlayFlashAlpha);
    }

    @Override
    public void tick(Minecraft client) {
        unclampedFlashAlpha += alphaDir * 0.125F;
        if (unclampedFlashAlpha >= 1.5F) {
            alphaDir = -1;
        } else if (unclampedFlashAlpha <= -0.5F) {
            alphaDir = 1;
        }
        flashAlpha = Math.max(0F, Math.min(1F, unclampedFlashAlpha))
                * (float) (double) maxHudOverlayFlashAlpha.get();
    }

    @Override
    protected void onDisable() {
        resetFlash();
    }

    /** Whether the overlays should mirror the vanilla HUD's bar bobbing animations. */
    public boolean showVanillaAnimations() {
        return showVanillaAnimationsOverlay.get();
    }

    /** Whether the offhand is checked for food when the main hand cannot be consumed. */
    public boolean checkOffhand() {
        return showFoodValuesHudOverlayWhenOffhand.get();
    }

    private void resetFlash() {
        unclampedFlashAlpha = flashAlpha = 0;
        alphaDir = 1;
    }

    /** Called before the vanilla food bar is drawn; used for the exhaustion underlay. */
    public void onExtractFoodPre(GuiGraphics graphics, Player player, int top, int right, int guiTicks) {
        if (!isEnabled() || player == null) return;
        if (!showFoodExhaustionHudUnderlay.get()) return;

        overlay.drawExhaustionOverlay(graphics, FoodHelper.exhaustionLevel(player.getFoodData()), right, top, 0.75F);
    }

    /** Called after the vanilla food bar is drawn; used for saturation + food-value overlays. */
    public void onExtractFoodPost(GuiGraphics graphics, Player player, int top, int right, int guiTicks) {
        if (!isEnabled() || player == null) return;
        if (!shouldRenderAnyOverlays()) return;

        FoodData stats = player.getFoodData();

        boolean showSaturation = showSaturationHudOverlay.get();
        if (showSaturation) {
            overlay.drawSaturationOverlay(graphics, 0, stats.getSaturationLevel(), player, right, top, 1.0F, guiTicks);
        }

        FoodHelper.QueriedFoodResult result = overlay.heldFood.result(guiTicks, player);
        if (result == null) {
            resetFlash();
            return;
        }

        if (showFoodValuesHudOverlay.get()) {
            // calculate the final hunger and saturation
            int foodHunger = result.modifiedFoodComponent.nutrition();
            float foodSaturationIncrement = result.modifiedFoodComponent.saturation();

            // draw hunger overlay
            overlay.drawHungerOverlay(graphics, foodHunger, stats.getFoodLevel(), player, right, top,
                    flashAlpha, FoodHelper.isRotten(result.itemStack), guiTicks);

            int newFoodValue = stats.getFoodLevel() + foodHunger;
            float newSaturationValue = stats.getSaturationLevel() + foodSaturationIncrement;

            // draw saturation overlay of gained saturation
            if (showSaturation) {
                float saturationGained = newSaturationValue > newFoodValue
                        ? newFoodValue - stats.getSaturationLevel()
                        : foodSaturationIncrement;
                overlay.drawSaturationOverlay(graphics, saturationGained, stats.getSaturationLevel(),
                        player, right, top, flashAlpha, guiTicks);
            }
        }
    }

    /** Called after the vanilla health bar is drawn; used for the estimated-health overlay. */
    public void onExtractHealth(GuiGraphics graphics, Player player, int left, int top, int guiTicks) {
        if (!isEnabled() || player == null) return;
        if (!shouldRenderAnyOverlays()) return;

        FoodHelper.QueriedFoodResult result = overlay.heldFood.result(guiTicks, player);
        if (result == null) {
            resetFlash();
            return;
        }

        if (shouldShowEstimatedHealth(player, guiTicks)) {
            float foodHealthIncrement = FoodHelper.getEstimatedHealthIncrement(player, result.itemStack, result.modifiedFoodComponent);
            float currentHealth = player.getHealth();
            float modifiedHealth = Math.min(currentHealth + foodHealthIncrement, player.getMaxHealth());

            if (currentHealth < modifiedHealth) {
                overlay.drawHealthOverlay(graphics, currentHealth, modifiedHealth, player, left, top, flashAlpha, guiTicks);
            }
        }
    }

    private boolean shouldRenderAnyOverlays() {
        return showFoodValuesHudOverlay.get() || showSaturationHudOverlay.get() || showFoodHealthHudOverlay.get();
    }

    private boolean shouldShowEstimatedHealth(Player player, int guiTicks) {
        if (!showFoodHealthHudOverlay.get()) {
            return false;
        }

        // Offsets size is set to zero intentionally to disable rendering when health is infinite.
        if (overlay.barOffsets.healthBarOffsets(guiTicks, player).isEmpty()) {
            return false;
        }

        FoodData stats = player.getFoodData();

        // in the PEACEFUL mode, health will restore faster
        if (player.level().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }

        // when the player has any changed health amount by any case, can't show estimated health
        // because the player will be confused about how much health would be restored/damaged
        if (stats.getFoodLevel() >= 18) {
            return false;
        }

        if (player.hasEffect(MobEffects.POISON)) {
            return false;
        }

        if (player.hasEffect(MobEffects.WITHER)) {
            return false;
        }

        if (player.hasEffect(MobEffects.REGENERATION)) {
            return false;
        }

        return true;
    }
}
