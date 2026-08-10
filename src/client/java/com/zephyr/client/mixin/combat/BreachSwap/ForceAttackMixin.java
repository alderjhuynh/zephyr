package com.zephyr.client.mixin.combat.BreachSwap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Mixin interface exposing the vanilla {@link Minecraft#startAttack()} as
 * {@code invokeDoAttack()}, used by the {@code BreachSwap}, {@code DensitySwap},
 * {@code LungeSwap} and {@code ShieldBreaker} modules to re-trigger an attack after swapping
 * to their preferred weapon.
 */
@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public interface ForceAttackMixin {
	/** Invoker for {@code Minecraft#startAttack()}; true if an attack was performed. */
	@Invoker("startAttack")
	boolean invokeDoAttack();
}
