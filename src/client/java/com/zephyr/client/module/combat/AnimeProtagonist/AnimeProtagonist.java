package com.zephyr.client.module.combat.AnimeProtagonist;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Teleports the local player behind an entity they just attacked by spoofing movement
 * packets. In DIRECT mode the player is moved onto the target's position; in DISTANCE mode
 * the player is moved to a point directly behind the target and looks at its eyes. Hooked
 * into the attack flow by the AnimeProtagonist mixins.
 */
public final class AnimeProtagonist extends Module {
    public static final AnimeProtagonist INSTANCE = new AnimeProtagonist();
    private final EnumSetting<AnimeProtagonist.Mode> mode = new EnumSetting<>("Mode", Mode.DISTANCE);

    /** Where the player should be relocated relative to the attacked entity. */
    public enum Mode {
        /** Move directly onto the target's current position. */
        DISTANCE,
        /** Move to a point behind the target and face it. */
        DIRECT
    }
    private AnimeProtagonist() {
        super("Anime Protagonist", "Attempts to teleport behind a hit entity", Category.COMBAT);
        addSetting(mode);
    }

    /** Entry point invoked by the AnimeProtagonist mixin right before the attack packet is sent; no-op when disabled. */
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
