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

public final class FastAttack extends Module {
    private static final int MIN_TIMES_PER_TICK = 1;
    private static final int MAX_TIMES_PER_TICK = 20;

    public static final FastAttack INSTANCE = new FastAttack();
    private FastAttack() {
        super("Fast Attack", "Simulates attack actions multiple times per tick", Category.QOL);
    }

    private final NumberSetting TimesPerTick =
            new NumberSetting("Actions Per Tick", 10, MIN_TIMES_PER_TICK, MAX_TIMES_PER_TICK, 1);

    @Override
    public void tick(Minecraft client) {
        for (int i = 0; i < TimesPerTick.get(); i++) {
            simulateAttack(client);
        }
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
