package com.zephyr.client.mixin.qol.AppleSkin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 26.2's {@link FoodData} keeps exhaustion in a private field with no getter.
 * This reads it so the exhaustion underlay has something to draw.
 *
 * <p>Backs the Zephyr AppleSkin module's exhaustion HUD underlay.
 */
@Mixin(FoodData.class)
public interface FoodDataAccessor {

    /**
     * Exposes {@link FoodData}'s private {@code exhaustionLevel} field so the
     * AppleSkin exhaustion underlay can read the current exhaustion value.
     *
     * @return the player's current food exhaustion level
     */
    @Accessor("exhaustionLevel")
    float zephyr$getExhaustionLevel();
}
