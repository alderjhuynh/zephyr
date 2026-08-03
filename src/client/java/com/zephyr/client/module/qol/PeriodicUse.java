package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
import com.zephyr.client.configplusgui.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class PeriodicUse extends Module {
    public static final PeriodicUse INSTANCE = new PeriodicUse();

    private static final double MIN_DELAY_TICKS = 1;
    private static final double MAX_DELAY_TICKS = 200;

    private final NumberSetting delayTicks =
            new NumberSetting("Delay (ticks)", 20, MIN_DELAY_TICKS, MAX_DELAY_TICKS, 1);

    private int tickCounter = 0;

    private PeriodicUse() {
        super("Periodic Use", "Automatically right-clicks on a fixed interval", Category.QOL);
        addSetting(delayTicks);
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) return;

        tickCounter++;

        double ticksDouble = delayTicks.get();
        int delayTicksInt = (int) ticksDouble;
        if (tickCounter >= delayTicksInt) {
            tickCounter = 0;
            simulateUse(client);
        }
    }

    @Override
    protected void onDisable() {
        tickCounter = 0;
    }

    private static void simulateUse(Minecraft client) {
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;

        if (player == null || gameMode == null) return;

        HitResult hit = client.hitResult;

        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            gameMode.useItemOn(player, InteractionHand.MAIN_HAND, (BlockHitResult) hit);
        } else {
            gameMode.useItem(player, InteractionHand.MAIN_HAND);
        }

        player.swing(InteractionHand.MAIN_HAND);
    }
}