package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Repeats an attack (entity hit / block hit / air swing) every N ticks while enabled. */
public final class PeriodicAttack extends Module {
    public static final PeriodicAttack INSTANCE = new PeriodicAttack();

    private static final double MIN_DELAY_TICKS = 1;
    private static final double MAX_DELAY_TICKS = 200;

    private final NumberSetting delayTicks =
            new NumberSetting("Delay (ticks)", 20, MIN_DELAY_TICKS, MAX_DELAY_TICKS, 1);

    private int tickCounter = 0;

    private PeriodicAttack() {
        super("Periodic Attack", "Automatically attacks on a fixed interval", Category.QOL);
        addSetting(delayTicks);
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) return;

        tickCounter++;

        double ticksDouble = delayTicks.get();
        int delayTicksInt = (int) ticksDouble;
        if (tickCounter >= (int) delayTicksInt) {
            tickCounter = 0;
            simulateAttack(client);
        }
    }

    @Override
    protected void onDisable() {
        tickCounter = 0;
    }

    private static void simulateAttack(Minecraft client) {
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        HitResult hit = client.hitResult;

        if (player == null || gameMode == null || hit == null) return;

        switch (hit.getType()) {
            case ENTITY -> {
                EntityHitResult entityHit = (EntityHitResult) hit;
                gameMode.attack(player, entityHit.getEntity());
                player.swing(InteractionHand.MAIN_HAND);
            }
            case BLOCK -> {
                BlockHitResult blockHit = (BlockHitResult) hit;
                gameMode.startDestroyBlock(blockHit.getBlockPos(), blockHit.getDirection());
                player.swing(InteractionHand.MAIN_HAND);
            }
            case MISS -> player.swing(InteractionHand.MAIN_HAND);
        }
    }
}