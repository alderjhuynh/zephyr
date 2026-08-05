package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public final class Criticals extends Module {
    public static final Criticals INSTANCE = new Criticals();
    NumberSetting FallDist = new NumberSetting("Fall Distance", 2D, 0.00D, 4.0D, 0.1D);
    private Criticals() {
        super("Criticals", "Creates falling packets to enable crits and mace slams", Category.COMBAT);
        addSetting(FallDist);
    }

    public static void onAttack() {
        Minecraft client = Minecraft.getInstance();
        if (!Criticals.INSTANCE.isEnabled() || client.player == null) return;

        if (!client.player.onGround()) return;
        if (client.player.onClimbable()) return;
        if (client.player.isInLava() || client.player.isInWater()) return;

        spoofCritPackets(client);
    }

    public static void forceCrit() {
        Minecraft client = Minecraft.getInstance();
        if (!Criticals.INSTANCE.isEnabled() || client.player == null) return;

        spoofCritPackets(client);
    }

    private static void spoofCritPackets(Minecraft client) {
        if (client.getConnection() == null) return;

        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();

        sendPos(x, y + Criticals.INSTANCE.FallDist.get(), z, false);
        sendPos(x, y, z, false);
    }

    private static void sendPos(double x, double y, double z, boolean onGround) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.getConnection() == null || mc.player == null)
            return;

        mc.getConnection().send(
                new ServerboundMovePlayerPacket.Pos(
                        x,
                        y,
                        z,
                        onGround,
                        false
                )
        );
    }
}
