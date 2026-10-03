package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class Jesus extends Module {
    public static final Jesus INSTANCE = new Jesus();

    private static final double FLOAT_SPEED = 0.1;

    private final BooleanSetting particles = new BooleanSetting("Particles", true);

    private Jesus() {
        super("Jesus", "Lets you walk on water as if it were solid ground", Category.MOVEMENT);
        addSetting(particles);
    }

    @Override
    public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.isInWater()) return;
        if (player.isShiftKeyDown() || player.isSwimming() || player.getAbilities().flying) return;

        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, Math.max(FLOAT_SPEED, movement.y), movement.z);
    }

    public boolean particles() {
        return particles.get();
    }
}
