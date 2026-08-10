package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class ElytraBoost extends Module {
    public static final ElytraBoost INSTANCE = new ElytraBoost();

    private final NumberSetting acceleration = new NumberSetting("Acceleration", 0.1D, 0.01D, 1.0D, 0.01D);

    private ElytraBoost() {
        super("Elytra Boost", "Accelerates while gliding", Category.MOVEMENT);
        addSetting(acceleration);
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player == null || !client.player.isFallFlying()) {
            return;
        }
        Vec3 direction = client.player.getLookAngle();
        client.player.addDeltaMovement(direction.scale(acceleration.get()));
    }
}
