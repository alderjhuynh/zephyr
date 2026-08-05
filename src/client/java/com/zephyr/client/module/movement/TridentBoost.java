package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Enables a Riptide trident to propel the local player even when dry. */
public final class TridentBoost extends Module {
    public static final TridentBoost INSTANCE = new TridentBoost();

    private static final int MIN_DRAW_DURATION = 10;
    private static final float UPWARD_BOOST = 1.2F;

    private TridentBoost() {
        super("Trident Boost", "Enables dry riptide boosts", Category.MOVEMENT);
    }

    public static boolean canUseOutsideWater(Player player, ItemStack stack) {
        return INSTANCE.isEnabled()
                && stack.is(Items.TRIDENT)
                && EnchantmentHelper.getTridentSpinAttackStrength(stack, player) > 0.0F
                && !player.isInWaterOrRain()
                && !stack.nextDamageWillBreak();
    }

    /**
     * Performs the velocity and sound portion of a vanilla Riptide release.
     * The item mixin calls this before the normal water-only check runs.
     */
    public static boolean handleDryRiptide(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player) || !canUseOutsideWater(player, stack)) {
            return false;
        }

        if (stack.getUseDuration(user) - remainingUseTicks < MIN_DRAW_DURATION) {
            return false;
        }

        float strength = EnchantmentHelper.getTridentSpinAttackStrength(stack, user);
        if (strength <= 0.0F) {
            return false;
        }

        float yaw = player.getYRot() * ((float) Math.PI / 180.0F);
        float pitch = player.getXRot() * ((float) Math.PI / 180.0F);
        float x = -Mth.sin(yaw) * Mth.cos(pitch);
        float y = -Mth.sin(pitch);
        float z = Mth.cos(yaw) * Mth.cos(pitch);
        float length = Mth.sqrt(x * x + y * y + z * z);

        Vec3 boost = new Vec3(x / length * strength, y / length * strength, z / length * strength);
        player.addDeltaMovement(boost);
        if (player.onGround()) {
            player.move(MoverType.SELF, new Vec3(0.0D, UPWARD_BOOST, 0.0D));
        }

        level.playSound(null, player, getRiptideSound(strength), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static SoundEvent getRiptideSound(float strength) {
        if (strength >= 3.0F) {
            return SoundEvents.TRIDENT_RIPTIDE_3.value();
        }
        if (strength >= 2.0F) {
            return SoundEvents.TRIDENT_RIPTIDE_2.value();
        }
        return SoundEvents.TRIDENT_RIPTIDE_1.value();
    }
}
