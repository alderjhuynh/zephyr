package com.zephyr.client.mixin.combat.TargetStrafe;

import com.zephyr.client.mixin.qol.GuiMove.ClientInputAccessor;
import com.zephyr.client.module.combat.TargetStrafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for {@link KeyboardInput} backing the {@code TargetStrafe} module. While the module
 * is enabled and a target is in range, the vanilla input computation is replaced with a pure
 * left/right strafe around the target: the player is first turned to face the target and the
 * move vector is redirected sideways so the player circles it. The vanilla body is cancelled
 * so the stale move vector is never recomputed.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    /**
     * Redirects movement into an orbit around the nearest target when the module is active
     * and the player is actually giving a movement input.
     *
     * @param ci mixin callback used to cancel the vanilla input computation
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void zephyr$targetStrafe(CallbackInfo ci) {
        TargetStrafe module = TargetStrafe.INSTANCE;
        if (!module.isEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        LivingEntity target = module.findTarget(client);
        if (target == null) return;

        Input current = player.input.keyPresses;
        if (!current.forward() && !current.backward() && !current.left() && !current.right()) return;
        if (module.onlyWhileAttacking() && !client.options.keyAttack.isDown()) return;

        player.setYRot(yawTo(player.getX(), player.getZ(), target.getX(), target.getZ()));

        boolean left = module.orbitLeft();
        player.input.keyPresses = new Input(false, false, left, !left, current.jump(), current.shift(), current.sprint());
        ((ClientInputAccessor) player.input).zephyr$setMoveVector(new Vec2(left ? -1.0F : 1.0F, 0.0F).normalized());

        ci.cancel();
    }

    /** Minecraft yaw (degrees) that makes an observer at {@code (fromX, fromZ)} face {@code (toX, toZ)}. */
    private static float yawTo(double fromX, double fromZ, double toX, double toZ) {
        return (float) Math.toDegrees(Math.atan2(fromX - toX, toZ - fromZ));
    }
}
