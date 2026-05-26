package com.zephyr.client.mixin.module.combat.LungeSwap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public interface ForceAttackMixin {
    @Invoker("doAttack")
    boolean invokeDoAttack();
}
