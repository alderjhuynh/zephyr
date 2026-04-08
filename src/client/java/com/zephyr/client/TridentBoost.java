package com.zephyr.client;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class TridentBoost {
    private static final int MIN_DRAW_DURATION = 10;
    private static final float RIPTIDE_UPWARD_BOOST = 1.1999999F;

    public static boolean enabled = true;

    private TridentBoost() {
    }

    public static boolean canUseOutsideWater(PlayerEntity player, ItemStack stack) {
        return enabled
                && stack.isOf(Items.TRIDENT)
                && hasRiptide(player, stack)
                && !player.isTouchingWaterOrRain()
                && !isAboutToBreak(stack);
    }

    public static boolean handleDryRiptide(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player) || !canUseOutsideWater(player, stack)) {
            return false;
        }

        int useTicks = stack.getMaxUseTime(user) - remainingUseTicks;
        if (useTicks < MIN_DRAW_DURATION) {
            return false;
        }

        float strength = EnchantmentHelper.getTridentSpinAttackStrength(stack, user);
        if (strength <= 0.0F) {
            return false;
        }

        float yaw = player.getYaw();
        float pitch = player.getPitch();
        float x = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        float y = -MathHelper.sin(pitch * 0.017453292F);
        float z = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        float magnitude = MathHelper.sqrt(x * x + y * y + z * z);

        x = x * (strength / magnitude);
        y = y * (strength / magnitude);
        z = z * (strength / magnitude);

        player.addVelocity(x, y, z);
        player.useRiptide(20, 8.0F, stack);

        if (player.isOnGround()) {
            player.move(MovementType.SELF, new Vec3d(0.0D, RIPTIDE_UPWARD_BOOST, 0.0D));
        }

        world.playSoundFromEntity(
                null,
                player,
                getRiptideSound(strength).value(),
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );
        return true;
    }

    private static boolean hasRiptide(LivingEntity user, ItemStack stack) {
        return EnchantmentHelper.getTridentSpinAttackStrength(stack, user) > 0.0F;
    }

    private static boolean isAboutToBreak(ItemStack stack) {
        return stack.isDamageable() && stack.getDamage() >= stack.getMaxDamage() - 1;
    }

    private static RegistryEntry<SoundEvent> getRiptideSound(float strength) {
        if (strength >= 3.0F) {
            return SoundEvents.ITEM_TRIDENT_RIPTIDE_3;
        }
        if (strength >= 2.0F) {
            return SoundEvents.ITEM_TRIDENT_RIPTIDE_2;
        }
        return SoundEvents.ITEM_TRIDENT_RIPTIDE_1;
    }
}
