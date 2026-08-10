package com.zephyr.client.module.qol.appleskin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of {@code squeek.appleskin.helpers.FoodHelper} (AppleSkin), adapted to
 * 26.2's Mojang-mapped API where food's status effects live on the
 * {@link Consumable} component instead of {@link FoodProperties}.
 */
public final class FoodHelper {
    private FoodHelper() {
    }

    /** Whether the given item stack is food (has the FOOD data component). */
    public static boolean isFood(ItemStack itemStack) {
        return itemStack.getComponents().has(DataComponents.FOOD);
    }

    /** Whether the player can currently eat the given food component. */
    public static boolean canConsume(Player player, FoodProperties foodComponent) {
        return player.canEat(foodComponent.canAlwaysEat());
    }

    public static FoodProperties EMPTY_FOOD_COMPONENT = new FoodProperties(0, 0, false);

    /**
     * Assumes itemStack is known to be food; always returns a non-null FoodProperties.
     */
    public static FoodProperties getDefaultFoodValues(ItemStack itemStack) {
        return itemStack.getOrDefault(DataComponents.FOOD, EMPTY_FOOD_COMPONENT);
    }

    /**
     * Holds the food properties of an item as stored on the item versus as
     * actually used (after any item-specific modification), together with the
     * stack itself.
     */
    public static class QueriedFoodResult {
        public FoodProperties defaultFoodComponent;
        public FoodProperties modifiedFoodComponent;
        public final ItemStack itemStack;

        public QueriedFoodResult(FoodProperties defaultFoodComponent, FoodProperties modifiedFoodComponent, ItemStack itemStack) {
            this.defaultFoodComponent = defaultFoodComponent;
            this.modifiedFoodComponent = modifiedFoodComponent;
            this.itemStack = itemStack;
        }
    }

    /**
     * Queries the food values of the given stack for the player, or returns
     * {@code null} if the stack is not food.
     *
     * @param itemStack the stack to query
     * @param player    the player who would eat it
     * @return the food values, or {@code null}
     */
    public static QueriedFoodResult query(ItemStack itemStack, Player player) {
        if (!isFood(itemStack)) return null;

        FoodProperties defaultFood = getDefaultFoodValues(itemStack);

        return new QueriedFoodResult(defaultFood, defaultFood, itemStack);
    }

    /**
     * Status effects that eating the item would apply. In 26.2 these moved from
     * FoodProperties onto the Consumable component as ApplyStatusEffectsConsumeEffect.
     */
    public static List<MobEffectInstance> getStatusEffects(ItemStack itemStack) {
        List<MobEffectInstance> effects = new ArrayList<>();
        Consumable consumable = itemStack.getOrDefault(DataComponents.CONSUMABLE, null);
        if (consumable == null) return effects;

        for (ConsumeEffect consumeEffect : consumable.onConsumeEffects()) {
            if (consumeEffect instanceof ApplyStatusEffectsConsumeEffect applyEffect) {
                effects.addAll(applyEffect.effects());
            }
        }
        return effects;
    }

    /** Whether eating the item applies any harmful status effect. */
    public static boolean isRotten(ItemStack itemStack) {
        for (MobEffectInstance effect : getStatusEffects(itemStack)) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                return true;
            }
        }
        return false;
    }

    /**
     * Estimates how much health eating the item would eventually restore, from
     * both natural regeneration and any regeneration effect the food applies.
     *
     * @param player         the player who would eat the item
     * @param itemStack      the food item
     * @param foodComponent  the food properties to use
     * @return the estimated health restored
     */
    public static float getEstimatedHealthIncrement(Player player, ItemStack itemStack, FoodProperties foodComponent) {
        if (!canFoodHeal(player)) {
            return 0;
        }

        FoodData stats = player.getFoodData();
        Level world = player.level();

        int foodLevel = Math.min(stats.getFoodLevel() + foodComponent.nutrition(), 20);
        float healthIncrement = 0;

        // health for natural regen
        if (foodLevel >= 18.0F && world != null && world.getServer() != null
                && world.getServer().getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION)) {
            float saturationLevel = Math.min(stats.getSaturationLevel() + foodComponent.saturation(), (float) foodLevel);
            float exhaustionLevel = exhaustionLevel(stats);
            healthIncrement = getEstimatedHealthIncrement(foodLevel, saturationLevel, exhaustionLevel);
        }

        // health for regeneration effect applied by the food
        for (MobEffectInstance effect : getStatusEffects(itemStack)) {
            if (effect.getEffect() == MobEffects.REGENERATION) {
                int amplifier = effect.getAmplifier();
                int duration = effect.getDuration();

                // Refer: https://minecraft.fandom.com/wiki/Regeneration
                // Refer: net.minecraft.world.effect.RegenerationMobEffect.canApplyUpdateEffect
                healthIncrement += (float) Math.floor(duration / Math.max(50 >> amplifier, 1));
                break;
            }
        }

        return healthIncrement;
    }

    public static float REGEN_EXHAUSTION_INCREMENT = 6.0F;
    public static float MAX_EXHAUSTION = 4.0F;

    /**
     * Simulates the vanilla health-regeneration loop for the given food and
     * exhaustion values to compute how much health would be restored.
     *
     * @param foodLevel        the food level after eating
     * @param saturationLevel  the saturation level after eating
     * @param exhaustionLevel  the current exhaustion level
     * @return the estimated health restored
     */
    public static float getEstimatedHealthIncrement(int foodLevel, float saturationLevel, float exhaustionLevel) {
        float health = 0;

        if (!Float.isFinite(exhaustionLevel) || !Float.isFinite(saturationLevel)) {
            return 0;
        }

        while (foodLevel >= 18) {
            while (exhaustionLevel > MAX_EXHAUSTION) {
                exhaustionLevel -= MAX_EXHAUSTION;
                if (saturationLevel > 0) {
                    saturationLevel = Math.max(saturationLevel - 1, 0);
                } else {
                    foodLevel -= 1;
                }
            }
            // Without this Float.compare, it's possible for this function to get stuck in an infinite loop
            // if saturationLevel is small enough that exhaustionLevel does not actually change representation
            // when it's incremented. This Float.compare makes it so we treat such close-to-zero values as zero.
            if (foodLevel >= 20 && Float.compare(saturationLevel, Float.MIN_NORMAL) > 0) {
                // fast regen health
                //
                // Because only health and exhaustionLevel increase in this branch,
                // we know that we will enter this branch again and again on each iteration
                // if exhaustionLevel is not incremented above MAX_EXHAUSTION before the
                // next iteration.
                //
                // So, instead of actually performing those iterations, we can calculate
                // the number of iterations it would take to reach max exhaustion, and
                // add all the health/exhaustion in one go. In practice, this takes the
                // worst-case number of iterations performed in this function from the millions
                // all the way down to around 18.
                float limitedSaturationLevel = Math.min(saturationLevel, REGEN_EXHAUSTION_INCREMENT);
                float exhaustionUntilAboveMax = Math.nextUp(MAX_EXHAUSTION) - exhaustionLevel;
                int numIterationsUntilAboveMax = Math.max(1, (int) Math.ceil(exhaustionUntilAboveMax / limitedSaturationLevel));

                health += (limitedSaturationLevel / REGEN_EXHAUSTION_INCREMENT) * numIterationsUntilAboveMax;
                exhaustionLevel += limitedSaturationLevel * numIterationsUntilAboveMax;
            } else if (foodLevel >= 18) {
                // slow regen health
                health += 1;
                exhaustionLevel += REGEN_EXHAUSTION_INCREMENT;
            }
        }

        return health;
    }

    /**
     * The client keeps its own local exhaustion estimate, but 26.2 exposes no
     * getter for it, so read it through the FoodDataAccessor mixin.
     */
    public static float exhaustionLevel(FoodData stats) {
        return ((com.zephyr.client.mixin.qol.AppleSkin.FoodDataAccessor) stats).zephyr$getExhaustionLevel();
    }

    private static boolean canFoodHeal(Player player) {
        MinecraftServer server = player.level().getServer();
        // On dedicated servers the client can't read game rules; assume natural
        // regeneration (the vanilla default) rather than hiding the overlay.
        return server == null || server.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);
    }
}
