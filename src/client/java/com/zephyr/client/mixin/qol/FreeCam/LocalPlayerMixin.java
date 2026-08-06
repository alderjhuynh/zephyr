package com.zephyr.client.mixin.qol.FreeCam;

import com.zephyr.client.module.qol.FreeCam;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Unique
    private boolean zephyr$freecamActive;
    @Unique
    private double zephyr$savedX;
    @Unique
    private double zephyr$savedY;
    @Unique
    private double zephyr$savedZ;
    @Unique
    private float zephyr$savedYRot;
    @Unique
    private float zephyr$savedXRot;

    @Inject(method = "raycastHitResult", at = @At("HEAD"))
    private void zephyr$freecamPickHead(float a, Entity cameraEntity, CallbackInfoReturnable<HitResult> cir) {
        zephyr$freecamActive = false;
        if (!FreeCam.INSTANCE.isEnabled()) return;

        FreeCam freecam = FreeCam.INSTANCE;
        if (freecam.getPos() == null) return;

        LocalPlayer player = (LocalPlayer) (Object) this;
        zephyr$savedX = player.getX();
        zephyr$savedY = player.getY();
        zephyr$savedZ = player.getZ();
        zephyr$savedYRot = player.getYRot();
        zephyr$savedXRot = player.getXRot();

        Vec3 eyePos = freecam.getPos();
        player.setPos(eyePos.x, eyePos.y - player.getEyeHeight(), eyePos.z);
        player.xo = player.getX();
        player.yo = player.getY();
        player.zo = player.getZ();
        player.setYRot(freecam.getYaw());
        player.setXRot(freecam.getPitch());
        player.yRotO = player.getYRot();
        player.xRotO = player.getXRot();
        zephyr$freecamActive = true;
    }

    @Inject(method = "raycastHitResult", at = @At("RETURN"))
    private void zephyr$freecamPickTail(float a, Entity cameraEntity, CallbackInfoReturnable<HitResult> cir) {
        if (!zephyr$freecamActive) return;
        zephyr$freecamActive = false;

        LocalPlayer player = (LocalPlayer) (Object) this;
        player.setPos(zephyr$savedX, zephyr$savedY, zephyr$savedZ);
        player.xo = zephyr$savedX;
        player.yo = zephyr$savedY;
        player.zo = zephyr$savedZ;
        player.setYRot(zephyr$savedYRot);
        player.setXRot(zephyr$savedXRot);
        player.yRotO = zephyr$savedYRot;
        player.xRotO = zephyr$savedXRot;
    }
}
