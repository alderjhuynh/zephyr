package com.zephyr.client.mixin.bot;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Mixin invoker into {@link Minecraft} exposing the private attack path used by
 * the SwordBot module ({@code com.zephyr.client.module.bot}): {@code pick} to
 * refresh the crosshair target and {@code startAttack} to swing without a mouse
 * click. Mirrors swordbot-v3's {@code MinecraftInputInvoker}.
 */
@Mixin(Minecraft.class)
public interface SwordBotMinecraftInvoker {
    /** Invokes the private {@code Minecraft.pick} method. */
    @Invoker("pick")
    void swordBot$pick(float partialTick);

    /** Invokes the private {@code Minecraft.startAttack} method. */
    @Invoker("startAttack")
    boolean swordBot$startAttack();
}
