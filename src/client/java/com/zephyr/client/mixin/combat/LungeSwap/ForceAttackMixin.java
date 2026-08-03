package com.zephyr.client.mixin.combat.LungeSwap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public interface ForceAttackMixin {
	@Invoker("startAttack")
	boolean invokeDoAttack();
}
