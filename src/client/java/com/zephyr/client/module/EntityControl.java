package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.JumpingMount;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import net.minecraft.util.math.Vec3d;

public class EntityControl {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean enabled = true;

    public static boolean spoofSaddle = true;
    public static boolean maxJump = true;
    public static boolean lockYaw = true;
    public static boolean cancelServerPackets = false;

    public static boolean speed = false;
    public static double horizontalSpeed = 10.0;
    public static boolean onlyOnGround = false;
    public static boolean inWater = true;

    public static boolean flight = true;
    public static double verticalSpeed = 6.0;
    public static double fallSpeed = 0.0;

    public static boolean antiKick = true;
    public static int delay = 40;

    private static int delayLeft = 0;
    private static double lastPacketY = Double.MAX_VALUE;
    private static boolean sentPacket = false;

    public void onEnable() {
        delayLeft = delay;
        sentPacket = false;
        lastPacketY = Double.MAX_VALUE;
    }

    public static void tick() {
        if (!enabled || mc.player == null) return;

        Entity vehicle = mc.player.getVehicle();

        if (sentPacket && vehicle != null) {
            VehicleMoveC2SPacket packet = VehicleMoveC2SPacket.fromVehicle(vehicle);

            //packet with modified y

            packet = new VehicleMoveC2SPacket(
                    new Vec3d(packet.position().x, lastPacketY, packet.position().z),
                    packet.yaw(),
                    packet.pitch(),
                    packet.onGround()
            );

            mc.getNetworkHandler().sendPacket(packet);
            sentPacket = false;
        }

        delayLeft--;
    }

    // TODO: entity.move() mixin

    public static Vec3d applyMovement(Entity entity, Vec3d movement) {
        if (!enabled || mc.player == null) return movement;
        if (entity.getControllingPassenger() != mc.player) return movement;

        double velX = movement.x;
        double velY = movement.y;
        double velZ = movement.z;

        if (speed &&
                (!onlyOnGround || entity.isOnGround() || entity.isFlyingVehicle()) &&
                (inWater || !entity.isTouchingWater())
        ) {
            Vec3d dir = getMovementInput(horizontalSpeed);
            velX = dir.x;
            velZ = dir.z;
        }

        if (flight) {
            velY = 0;

            GameOptions options = mc.options;

            if (options.jumpKey.isPressed()) {
                velY += verticalSpeed / 20.0;
            }

            if (options.sprintKey.isPressed()) {
                velY -= verticalSpeed / 20.0;
            } else {
                velY -= fallSpeed / 20.0;
            }
        }

        if (lockYaw) {
            entity.setYaw(mc.player.getYaw());
        }

        return new Vec3d(velX, velY, velZ);
    }

    //TODO: hook into ClientPlayNetworkHandler.sendPacket

    public static void onSendPacket(Object packetObj) {
        if (!enabled || !antiKick) return;
        if (!(packetObj instanceof VehicleMoveC2SPacket packet)) return;

        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return;

        double currentY = packet.position().y;

        if (delayLeft <= 0 &&
                !sentPacket &&
                shouldFlyDown(currentY) &&
                vehicle.isOnGround() == false &&
                !vehicle.isFlyingVehicle()
        ) {
            VehicleMoveC2SPacket modified = new VehicleMoveC2SPacket(
                    new Vec3d(packet.position().x, lastPacketY-0.03130D, packet.position().z),
                    packet.yaw(),
                    packet.pitch(),
                    packet.onGround()
            );

            mc.getNetworkHandler().sendPacket(modified);

            sentPacket = true;
            delayLeft = delay;
        }

        lastPacketY = currentY;
    }


    //TODO: hook into ClientPlayNetworkHandler.onVehicleMove

    public static boolean onReceivePacket(Object packet) {
        if (!enabled) return false;

        return packet instanceof VehicleMoveS2CPacket && cancelServerPackets;
    }

    private static boolean shouldFlyDown(double currentY) {
        if (currentY >= lastPacketY) return true;
        return lastPacketY - currentY < 0.03130D;
    }

    private static Vec3d getMovementInput(double speed) {
        float yaw = mc.player.getYaw();

        double forward = 0;
        double strafe = 0;

        if (mc.options.forwardKey.isPressed()) forward += 1;
        if (mc.options.backKey.isPressed()) forward -= 1;
        if (mc.options.leftKey.isPressed()) strafe += 1;
        if (mc.options.rightKey.isPressed()) strafe -= 1;

        if (forward == 0 && strafe == 0) return Vec3d.ZERO;

        double rad = Math.toRadians(yaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        double x = (forward * cos - strafe * sin) * speed / 20.0;
        double z = (forward * sin + strafe * cos) * speed / 20.0;

        return new Vec3d(x, 0, z);
    }


    public boolean spoofSaddle() {
        return enabled && spoofSaddle;
    }

    public boolean maxJump() {
        return enabled && maxJump;
    }

    public boolean cancelJump() {
        Entity vehicle = mc.player.getVehicle();
        if (!(vehicle instanceof JumpingMount)) return false;

        return enabled && flight;
    }
}