package com.zephyr.client.mixin.combat.TargetStrafe;

import com.zephyr.client.module.combat.TargetStrafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Backport of 26.3's TargetStrafe mixin. 1.21.1 differences: player.input is
// the classic Input (public up/down/left/right/jumping/shiftKeyDown fields +
// impulses, move vector derived from impulses), not 26.x ClientInput with
// keyPresses/moveVector records.
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void zephyr$targetStrafe(boolean sprinting, float sneakPenalty, CallbackInfo ci) {
        TargetStrafe module = TargetStrafe.INSTANCE;
        if (!module.isEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        LivingEntity target = module.findTarget(client);
        if (target == null) return;

        Input current = player.input;
        if (!current.up && !current.down && !current.left && !current.right) return;
        if (module.onlyWhileAttacking() && !client.options.keyAttack.isDown()) return;

        player.setYRot(yawTo(player.getX(), player.getZ(), target.getX(), target.getZ()));

        boolean left = module.orbitLeft();
        current.up = false;
        current.down = false;
        current.left = left;
        current.right = !left;
        current.forwardImpulse = 0.0F;
        current.leftImpulse = left ? 1.0F : -1.0F;

        ci.cancel();
    }

    private static float yawTo(double fromX, double fromZ, double toX, double toZ) {
        return (float) Math.toDegrees(Math.atan2(fromX - toX, toZ - fromZ));
    }
}
