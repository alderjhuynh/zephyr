package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;

public final class Flight extends Module {
    public static final Flight INSTANCE = new Flight();

    private Flight() {
        super("Flight", "Enables client flight", Category.MOVEMENT);
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player != null && !client.player.isSpectator()) {
            client.player.getAbilities().flying = true;
            client.player.getAbilities().mayfly = true;
        }
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && !client.player.isCreative() && !client.player.isSpectator()) {
            client.player.getAbilities().flying = false;
            client.player.getAbilities().mayfly = false;
        }
    }
}
