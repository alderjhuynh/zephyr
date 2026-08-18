package com.zephyr.client.mixin.bots.pathing;

import com.zephyr.client.mixin.qol.GuiMove.ClientInputAccessor;
import com.zephyr.client.module.bots.pathing.Pathing;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link KeyboardInput} backing the {@code Pathing} module. While the
 * module is active it forces the forward key at the tail of the vanilla input
 * computation, forwards the module's jump intent, and sets a pure forward move
 * vector so the player walks along the path steered by the module's tick.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    /**
     * Overrides the vanilla input with a forced forward press when pathing is active.
     *
     * @param ci mixin callback info (unused)
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void zephyr$pathing(CallbackInfo ci) {
        Pathing module = Pathing.INSTANCE;
        if (!module.isEnabled() || !module.isActive()) return;

        ClientInputAccessor accessor = (ClientInputAccessor) this;
        Input current = accessor.zephyr$getKeyPresses();
        accessor.zephyr$setKeyPresses(new Input(
                true, false, current.left(), current.right(),
                module.wantsJump(), false, current.sprint()));
        accessor.zephyr$setMoveVector(new Vec2(0.0F, 1.0F).normalized());
    }
}