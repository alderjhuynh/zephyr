package com.zephyr.client.mixin.combat;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractArrow.class)
public interface AbstractArrowInvoker {

    @Accessor("inGround")
    boolean zephyr$isInGround();
}
