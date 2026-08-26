package com.zephyr.client.module.combat;

/**
 * Shared re-entrancy guard for {@code BreachSwap} and {@code DensitySwap} attack swaps.
 * Both {@code MultiPlayerGameMode#attack} mixins re-trigger the attack via
 * {@code Minecraft#startAttack} which re-enters {@code attack}. A single static flag
 * prevents the nested re-entry from swapping again and sharing {@code previousSlot}
 * keeps restore order correct regardless of which mixin swapped first.
 * This also fixes the tick-registration order dependency: {@code BreachSwap} is
 * registered before {@code DensitySwap} in {@code ModuleManager}, so without a
 * shared guard the two separate {@code isProcessingAttack} fields would let a
 * nested {@code Breach} swap overwrite a {@code Density} swap.
 *
 * <p>This class must NOT live in {@code com.zephyr.client.mixin.*} because the
 * Fabric Mixin loader forbids direct references to classes inside a defined mixin
 * package. Keeping it in the module package allows both mixins to reference it
 * safely.</p>
 */
public final class MaceSwapGuard {
    private MaceSwapGuard() {}

    public static boolean isProcessingAttack = false;
    public static int previousSlot = -1;
}
