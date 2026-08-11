package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Movement module that lets the player walk on water. The Jesus LivingEntity mixin makes
 * {@code LivingEntity.canStandOnFluid} report water as standable and provides a solid
 * collision surface on top of the water, so the game treats the water surface as ground
 * while the module is enabled. This tick pushes the player up whenever they are submerged,
 * so they float back to the surface instead of sinking to the bottom. Sneaking or swimming
 * disables the behavior so diving and swimming out still work normally.
 */
public final class Jesus extends Module {
    public static final Jesus INSTANCE = new Jesus();

    /** Upward velocity applied each tick while submerged, so the player rises to the surface. */
    private static final double FLOAT_SPEED = 0.1;

    private final BooleanSetting particles = new BooleanSetting("Particles", true);

    private Jesus() {
        super("Jesus", "Lets you walk on water as if it were solid ground", Category.MOVEMENT);
        addSetting(particles);
    }

    /** Lifts the player toward the water's surface while they are submerged. */
    @Override
    public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.isInWater()) return;
        if (player.isShiftKeyDown() || player.isSwimming() || player.getAbilities().flying) return;

        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, Math.max(FLOAT_SPEED, movement.y), movement.z);
    }

    /** Whether to keep the vanilla water-splash particles while walking on water. */
    public boolean particles() {
        return particles.get();
    }
}
