package com.zephyr.client.mixin.qol.PickBeforePlace;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Mixin invoker into {@link Minecraft} exposing the private
 * {@code pickBlockOrEntity} method.
 *
 * <p>Used by the PickBeforePlace {@link BlockPlaceMixin} to force a pick-block
 * action right before a block is placed.
 */
@Mixin(Minecraft.class)
public interface BlockPickInvoker {
    /**
     * Invokes the private {@code Minecraft.pickBlockOrEntity} method.
     */
    @Invoker("pickBlockOrEntity")
    void invokePickBlock();
}
