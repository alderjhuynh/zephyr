package com.zephyr.client.mixin;

import com.zephyr.client.ZephyrClient;
import com.zephyr.client.module.EntityControl;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMoveMixin {

    private static final ThreadLocal<Boolean> MOVING = ThreadLocal.withInitial(() -> false);

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void onMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (MOVING.get()) return;

        Entity self = (Entity)(Object)this;
        Vec3d modified = EntityControl.applyMovement(self, movement);;

        if (!modified.equals(movement)) {
            MOVING.set(true);
            try {
                ci.cancel();
                self.move(type, modified);
            } finally {
                MOVING.set(false);
            }
        }
    }
}
