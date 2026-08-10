package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Replays the player's item-use action several times per tick to speed up
 * actions such as eating and block placement. Each tick, the configured number
 * of simulated uses is performed, either on the targeted block or with the held
 * item directly.
 */
public final class FastUse extends Module {
    private static final int MIN_TIMES_PER_TICK = 1;
    private static final int MAX_TIMES_PER_TICK = 20;

    public static final FastUse INSTANCE = new FastUse();
    private FastUse() {
        super("Fast Use", "Simulates use actions multiple times per tick", Category.QOL);
    }

    private final NumberSetting TimesPerTick =
            new NumberSetting("Actions Per Tick", 10, MIN_TIMES_PER_TICK, MAX_TIMES_PER_TICK, 1);

    /**
     * Performs the configured number of simulated use actions for the current tick.
     *
     * @param client the Minecraft client instance
     */
    @Override
    public void tick(Minecraft client) {
        for (int i = 0; i < TimesPerTick.get(); i++) {
            simulateUse(client);
        }
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
