package com.zephyr.client.module.qol.appleskin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.zephyr.client.module.qol.AppleSkin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of {@code squeek.appleskin.client.HUDOverlayHandler} (AppleSkin). Draws
 * the saturation / exhaustion / food-restored / health-restored overlays on top
 * of the vanilla HUD, replicating the vanilla bar offsets with the same PRNG
 * seed so the overlays line up with the bars they describe.
 */
public class HudOverlayHandler {
    public final OffsetsCache barOffsets = new OffsetsCache();
    public final HeldFoodCache heldFood = new HeldFoodCache();

    private static final RenderPipeline GUI_TEXTURED = RenderPipelines.GUI_TEXTURED;

    public void drawSaturationOverlay(GuiGraphicsExtractor graphics, float saturationGained, float saturationLevel,
                                      Player player, int right, int top, float alpha, int guiTicks) {
        if (saturationLevel + saturationGained < 0) {
            return;
        }

        float modifiedSaturation = Math.max(0, Math.min(saturationLevel + saturationGained, 20));

        int startSaturationBar = 0;
        int endSaturationBar = (int) Math.ceil(modifiedSaturation / 2.0F);

        // when requiring rendering of the gained saturation, start should relocate to current saturation tail.
        if (saturationGained != 0) {
            startSaturationBar = (int) Math.max(saturationLevel / 2.0F, 0);
        }

        int iconSize = 9;

        List<IntPoint> foodBarOffsets = barOffsets.foodBarOffsets(guiTicks, player);
        for (int i = startSaturationBar; i < endSaturationBar; ++i) {
            IntPoint offset = i < foodBarOffsets.size() ? foodBarOffsets.get(i) : null;
            if (offset == null) {
                continue;
            }

            int x = right + offset.x;
            int y = top + offset.y;

            int v = 0;
            int u = 0;

            float effectiveSaturationOfBar = (modifiedSaturation / 2.0F) - i;

            if (effectiveSaturationOfBar >= 1) {
                u = 3 * iconSize;
            } else if (effectiveSaturationOfBar > .5) {
                u = 2 * iconSize;
            } else if (effectiveSaturationOfBar > .25) {
                u = 1 * iconSize;
            }

            graphics.blit(GUI_TEXTURED, TextureHelper.MOD_ICONS, x, y, u, v, iconSize, iconSize, 256, 256, ARGB.white(alpha));
        }
    }

    public void drawHungerOverlay(GuiGraphicsExtractor graphics, int hungerRestored, int foodLevel,
                                  Player player, int right, int top, float alpha, boolean useRottenTextures, int guiTicks) {
        if (hungerRestored <= 0) {
            return;
        }

        int modifiedFood = Math.max(0, Math.min(20, foodLevel + hungerRestored));

        int startFoodBars = Math.max(0, foodLevel / 2);
        int endFoodBars = (int) Math.ceil(modifiedFood / 2.0F);

        int iconSize = 9;

        List<IntPoint> foodBarOffsets = barOffsets.foodBarOffsets(guiTicks, player);
        for (int i = startFoodBars; i < endFoodBars; ++i) {
            IntPoint offset = i < foodBarOffsets.size() ? foodBarOffsets.get(i) : null;
            if (offset == null) {
                continue;
            }

            int x = right + offset.x;
            int y = top + offset.y;

            Identifier backgroundSprite = TextureHelper.getFoodTexture(useRottenTextures, TextureHelper.FoodType.EMPTY);

            // very faint background
            graphics.blitSprite(GUI_TEXTURED, backgroundSprite, x, y, iconSize, iconSize, ARGB.white(alpha * 0.25F));

            boolean isHalf = i * 2 + 1 == modifiedFood;
            Identifier iconSprite = TextureHelper.getFoodTexture(useRottenTextures, isHalf ? TextureHelper.FoodType.HALF : TextureHelper.FoodType.FULL);

            graphics.blitSprite(GUI_TEXTURED, iconSprite, x, y, iconSize, iconSize, ARGB.white(alpha));
        }
    }

    public void drawHealthOverlay(GuiGraphicsExtractor graphics, float health, float modifiedHealth,
                                  Player player, int right, int top, float alpha, int guiTicks) {
        if (modifiedHealth <= health) {
            return;
        }

        int fixedModifiedHealth = (int) Math.ceil(modifiedHealth);
        boolean isHardcore = player.level() != null && player.level().getLevelData().isHardcore();

        int startHealthBars = (int) Math.max(0, (Math.ceil(health) / 2.0F));
        int endHealthBars = (int) Math.max(0, Math.ceil(modifiedHealth / 2.0F));

        int iconSize = 9;

        List<IntPoint> healthBarOffsets = barOffsets.healthBarOffsets(guiTicks, player);
        for (int i = startHealthBars; i < endHealthBars; ++i) {
            IntPoint offset = i < healthBarOffsets.size() ? healthBarOffsets.get(i) : null;
            if (offset == null) {
                continue;
            }

            int x = right + offset.x;
            int y = top + offset.y;

            Identifier backgroundSprite = TextureHelper.getHeartTexture(isHardcore, TextureHelper.HeartType.CONTAINER);

            // very faint background
            graphics.blitSprite(GUI_TEXTURED, backgroundSprite, x, y, iconSize, iconSize, ARGB.white(alpha * 0.25F));

            boolean isHalf = i * 2 + 1 == fixedModifiedHealth;
            Identifier iconSprite = TextureHelper.getHeartTexture(isHardcore, isHalf ? TextureHelper.HeartType.HALF : TextureHelper.HeartType.FULL);

            graphics.blitSprite(GUI_TEXTURED, iconSprite, x, y, iconSize, iconSize, ARGB.white(alpha));
        }
    }

    public void drawExhaustionOverlay(GuiGraphicsExtractor graphics, float exhaustion, int right, int top, float alpha) {
        float maxExhaustion = FoodHelper.MAX_EXHAUSTION;
        // clamp between 0 and 1
        float ratio = Math.min(1, Math.max(0, exhaustion / maxExhaustion));
        int width = (int) (ratio * 81);
        int height = 9;

        graphics.blit(GUI_TEXTURED, TextureHelper.MOD_ICONS, right - width, top, 81 - width, 18, width, height, 256, 256, ARGB.white(alpha));
    }

    /**
     * Replicates the vanilla HUD's bar-offset generation (same PRNG seed, same
     * consumption order: health first, then food) so overlays align with the
     * actual food/heart bars. See {@code Hud.extractPlayerHealth}.
     */
    public static class OffsetsCache {
        protected final List<IntPoint> foodBarOffsets = new ArrayList<>();
        protected final List<IntPoint> healthBarOffsets = new ArrayList<>();
        public int lastGuiTick = 0;
        protected final RandomSource random = RandomSource.create();

        protected void generate(int guiTicks, Player player) {
            final int preferHealthBars = 10;
            final int preferFoodBars = 10;

            final float maxHealth = player.getMaxHealth();
            final float absorptionHealth = (float) Math.ceil(player.getAbsorptionAmount());

            int healthBars = (int) Math.ceil((maxHealth + absorptionHealth) / 2.0F);
            // When maxHealth + absorptionHealth is greater than Integer.MAX_VALUE,
            // Minecraft will disable heart rendering due to a quirk of Mth.ceil.
            // We have a much lower threshold since there's no reason to get the offsets
            // for thousands of hearts.
            if (healthBars < 0 || healthBars > 1000) {
                healthBars = 0;
            }

            int healthRows = (int) Math.ceil((float) healthBars / (float) preferHealthBars);
            int healthRowHeight = Math.max(10 - (healthRows - 2), 3);

            FoodData hungerManager = player.getFoodData();

            boolean shouldAnimatedHealth = false;
            boolean shouldAnimatedFood = false;

            if (AppleSkin.INSTANCE.showVanillaAnimations()) {
                // in vanilla, when saturation is zero the food bar bobs
                float saturationLevel = hungerManager.getSaturationLevel();
                int foodLevel = hungerManager.getFoodLevel();
                shouldAnimatedFood = saturationLevel <= 0.0F && guiTicks % (foodLevel * 3 + 1) == 0;

                // in vanilla, health too low (below 5) shows the heartbeat animation
                shouldAnimatedHealth = Math.ceil(player.getHealth()) <= 4;
            }

            // hard coded in `Hud`
            random.setSeed((long) (guiTicks * 312871));

            // adjust the size
            if (healthBarOffsets.size() != healthBars) {
                healthBarOffsets.clear();
                for (int i = 0; i < healthBars; i++) {
                    healthBarOffsets.add(new IntPoint());
                }
            }

            if (foodBarOffsets.size() != preferFoodBars) {
                foodBarOffsets.clear();
                for (int i = 0; i < preferFoodBars; i++) {
                    foodBarOffsets.add(new IntPoint());
                }
            }

            // left alignment, multiple rows, reverse
            for (int i = healthBars - 1; i >= 0; --i) {
                int row = (int) Math.ceil((float) (i + 1) / (float) preferHealthBars) - 1;
                int x = i % preferHealthBars * 8;
                int y = -(row * healthRowHeight);
                // apply the animated offset
                if (shouldAnimatedHealth) {
                    y += random.nextInt(2);
                }

                IntPoint point = healthBarOffsets.get(i);
                point.x = x;
                point.y = y;
            }

            // right alignment, single row
            for (int i = 0; i < preferFoodBars; ++i) {
                int x = -(i * 8) - 9;
                int y = 0;

                // apply the animated offset
                if (shouldAnimatedFood) {
                    y += random.nextInt(3) - 1;
                }

                IntPoint point = foodBarOffsets.get(i);
                point.x = x;
                point.y = y;
            }
        }

        public List<IntPoint> healthBarOffsets(int guiTick, Player player) {
            if (guiTick != lastGuiTick) {
                generate(guiTick, player);
                lastGuiTick = guiTick;
            }
            return healthBarOffsets;
        }

        public List<IntPoint> foodBarOffsets(int guiTicks, Player player) {
            if (guiTicks != lastGuiTick) {
                generate(guiTicks, player);
                lastGuiTick = guiTicks;
            }
            return foodBarOffsets;
        }
    }

    /**
     * Caches the queried food in the player's hands once per GUI tick.
     */
    public static class HeldFoodCache {
        protected FoodHelper.QueriedFoodResult result;
        public int lastGuiTick = 0;

        protected void query(Player player) {
            // try to get the item stack in the player hand
            ItemStack heldItem = player.getMainHandItem();
            FoodHelper.QueriedFoodResult heldFood = FoodHelper.query(heldItem, player);
            boolean canConsume = heldFood != null && FoodHelper.canConsume(player, heldFood.modifiedFoodComponent);
            if (AppleSkin.INSTANCE.checkOffhand() && !canConsume) {
                heldItem = player.getOffhandItem();
                heldFood = FoodHelper.query(heldItem, player);
                canConsume = heldFood != null && FoodHelper.canConsume(player, heldFood.modifiedFoodComponent);
            }

            boolean shouldRenderHeldItemValues = !heldItem.isEmpty() && canConsume;
            if (!shouldRenderHeldItemValues) {
                this.result = null;
                return;
            }

            this.result = heldFood;
        }

        public FoodHelper.QueriedFoodResult result(int guiTick, Player player) {
            if (guiTick != lastGuiTick) {
                query(player);
                lastGuiTick = guiTick;
            }
            return result;
        }
    }
}
