package com.zephyr.client.mixin.combat;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Mixin interface exposing {@link AbstractArrow#isInGround()} as {@code zephyr$isInGround()},
 * which is protected in vanilla. Used by the {@code InstaCart} and {@code XBowCart} modules
 * to detect whether an arrow has already hit the ground.
 */
@Mixin(AbstractArrow.class)
public interface AbstractArrowInvoker {

    /** Invoker for {@code AbstractArrow#isInGround()}; true if the arrow is stuck in a block. */
    @Invoker("isInGround")
    boolean zephyr$isInGround();
}
