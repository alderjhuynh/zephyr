package com.zephyr.client.mixin;

import com.zephyr.client.module.F5Tweaks;
import com.zephyr.client.module.FreeCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private Entity focusedEntity;
    @Shadow private float cameraY;
    @Shadow private float lastCameraY;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void moveBy(float x, float y, float z);

    @Inject(method = "update", at = @At("TAIL"))
    private void f5mod$applyFreeLook(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(this.focusedEntity instanceof ClientPlayerEntity player)) {
            return;
        }

        if (FreeCam.isActive()) {
            this.setRotation(FreeCam.getYaw(), FreeCam.getPitch());
            this.setPos(
                    FreeCam.getRenderX(tickDelta),
                    FreeCam.getRenderY(tickDelta),
                    FreeCam.getRenderZ(tickDelta)
            );
            return;
        }

        if (!F5Tweaks.shouldFreeLook(client.options.getPerspective())) {
            return;
        }

        F5Tweaks.ensureInitialized(player.getYaw(), player.getPitch());
        this.setRotation(F5Tweaks.cameraYaw, F5Tweaks.cameraPitch);

        double x = MathHelper.lerp(tickDelta, this.focusedEntity.lastX, this.focusedEntity.getX());
        double y = MathHelper.lerp(tickDelta, this.focusedEntity.lastY, this.focusedEntity.getY())
                + MathHelper.lerp(tickDelta, this.lastCameraY, this.cameraY);
        double z = MathHelper.lerp(tickDelta, this.focusedEntity.lastZ, this.focusedEntity.getZ());
        this.setPos(x, y, z);

        float scale = this.focusedEntity instanceof LivingEntity livingEntity ? livingEntity.getScale() : 1.0F;
        this.moveBy(-4.0F * scale, 0.0F, 0.0F);
    }
}
