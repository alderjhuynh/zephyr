package com.zephyr.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class ElytraBoost {
    private static final double BOOST_DIRECTIONAL_ACCELERATION = 0.1D;
    private static final double BOOST_TARGET_SPEED = 1.5D;
    private static final double BOOST_BLEND = 0.5D;

    public static boolean enabled = true;
    public static boolean dontConsumeFirework = true;
    public static int fireworkLevel = 1;
    public static boolean playSound = true;

    private static int remainingBoostTicks;

    public static void startBoost(MinecraftClient mc) {
        if (!canStartBoost(mc, mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandStack())
                && !canStartBoost(mc, mc.player == null ? ItemStack.EMPTY : mc.player.getOffHandStack())) {
            return;
        }

        PlayerEntity player = mc.player;
        if (player == null) {
            return;
        }

        remainingBoostTicks = getLifetimeTicks(player);

        if (playSound) {
            mc.world.playSoundFromEntity(
                    player,
                    player,
                    SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH,
                    SoundCategory.AMBIENT,
                    3.0F,
                    1.0F
            );
        }
    }

    public static boolean isBlockingRockets(ItemStack stack) {
        return enabled && stack.isOf(Items.FIREWORK_ROCKET);
    }

    public static void tick(MinecraftClient mc) {
        if (remainingBoostTicks <= 0) {
            return;
        }

        if (!enabled || !canBoost(mc)) {
            remainingBoostTicks = 0;
            return;
        }

        PlayerEntity player = mc.player;
        if (player == null) {
            remainingBoostTicks = 0;
            return;
        }

        applyVanillaFireworkBoost(player);
        spawnFireworkParticles(mc, player);
        remainingBoostTicks--;
    }

    private static boolean canBoost(MinecraftClient mc) {
        return mc.player != null
                && mc.world != null
                && mc.currentScreen == null
                && mc.player.isFallFlying();
    }

    public static boolean canStartBoost(MinecraftClient mc, ItemStack stack) {
        return isBlockingRockets(stack)
                && dontConsumeFirework
                && canBoost(mc);
    }

    private static int getLifetimeTicks(PlayerEntity player) {
        int flight = Math.max(fireworkLevel, 0);
        return 10 * (1 + flight) + player.getRandom().nextInt(6) + player.getRandom().nextInt(7);
    }

    private static void applyVanillaFireworkBoost(PlayerEntity player) {
        Vec3d rotation = player.getRotationVector();
        Vec3d velocity = player.getVelocity();

        player.setVelocity(velocity.add(
                rotation.x * BOOST_DIRECTIONAL_ACCELERATION + (rotation.x * BOOST_TARGET_SPEED - velocity.x) * BOOST_BLEND,
                rotation.y * BOOST_DIRECTIONAL_ACCELERATION + (rotation.y * BOOST_TARGET_SPEED - velocity.y) * BOOST_BLEND,
                rotation.z * BOOST_DIRECTIONAL_ACCELERATION + (rotation.z * BOOST_TARGET_SPEED - velocity.z) * BOOST_BLEND
        ));
    }

    private static void spawnFireworkParticles(MinecraftClient mc, PlayerEntity player) {
        if ((remainingBoostTicks & 1) != 0) {
            return;
        }

        Vec3d velocity = player.getVelocity();
        mc.world.addParticle(
                ParticleTypes.FIREWORK,
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getRandom().nextGaussian() * 0.05D,
                -velocity.y * 0.5D,
                player.getRandom().nextGaussian() * 0.05D
        );
    }
}
