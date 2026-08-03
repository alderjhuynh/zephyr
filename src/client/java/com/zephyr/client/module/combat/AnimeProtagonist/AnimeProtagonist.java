package com.zephyr.client.module.combat.AnimeProtagonist;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.EnumSetting;
import com.zephyr.client.configplusgui.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class AnimeProtagonist extends Module {
    public static final AnimeProtagonist INSTANCE = new AnimeProtagonist();
    private final EnumSetting<AnimeProtagonist.Mode> mode = new EnumSetting<>("Mode", Mode.DISTANCE);
    public enum Mode {
        DISTANCE,
        DIRECT
    }
    private AnimeProtagonist() {
        super("Anime Protagonist", "Attempts to teleport behind a hit entity", Category.COMBAT);
        addSetting(mode);
    }

    public static void onAttack() {
        Minecraft client = Minecraft.getInstance();
        if (!AnimeProtagonist.INSTANCE.isEnabled() || client.player == null) return;
        spoofMovementPackets(client);
    }

    private static void spoofMovementPackets(Minecraft client) {
        if (client.getConnection() == null) return;

        LivingEntity target = TargetManager.getTarget();
        if (target == null) return;

        if (AnimeProtagonist.INSTANCE.mode.get() == Mode.DIRECT) {
            Vec3 targetPos = target.position();
            double x = targetPos.x;
            double y = targetPos.y;
            double z = targetPos.z;
            sendPos(x, y , z, false);
            client.player.setPos(x, y, z);
        }

        if (AnimeProtagonist.INSTANCE.mode.get() == Mode.DISTANCE) {
            double distance = client.player.distanceTo(target);
            Vec3 look = target.getLookAngle();
            look = new Vec3(look.x, 0.0, look.z).normalize();
            Vec3 behind = target.position().subtract(look.scale(distance));

            Vec3 targetEyePos = target.getEyePosition();

            double x = behind.x;
            double y = target.getY();
            double z = behind.z;

            // logical pos
            sendPos(x, y , z, false);
            // visual pos
            client.player.setPos(x, y, z);
            client.player.lookAt(EntityAnchorArgument.Anchor.EYES, targetEyePos);

            TargetManager.clear();
        }
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
