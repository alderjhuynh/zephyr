package com.zephyr.client.mixin.qol.AppleSkin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 26.2's {@link FoodData} keeps exhaustion in a private field with no getter.
 * This reads it so the exhaustion underlay has something to draw.
 */
@Mixin(FoodData.class)
public interface FoodDataAccessor {

    @Accessor("exhaustionLevel")
    float zephyr$getExhaustionLevel();
}
