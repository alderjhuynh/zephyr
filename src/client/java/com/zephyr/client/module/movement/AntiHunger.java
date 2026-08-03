package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.Category;
import com.zephyr.client.configplusgui.Module;
import com.zephyr.client.mixin.movement.NoFall.PlayerMoveC2SPacketAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public final class AntiHunger extends Module {
    public static final AntiHunger INSTANCE = new AntiHunger();

    private static boolean lastOnGround;
    private static boolean ignoreNextMovePacket;

    private AntiHunger() {
        super("Anti Hunger", "Avoids unnecessary sprint packets", Category.MOVEMENT);
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player == null) {
            return;
        }

        boolean onGround = client.player.onGround();
        if (onGround && !lastOnGround) {
            ignoreNextMovePacket = true;
        }
        lastOnGround = onGround;
    }

    /**
     * @return whether the packet should be sent. Called by the connection mixin
     * before the packet is written to the network channel.
     */
    public static boolean onSendPacket(Packet<?> packet, Minecraft client) {
        if (!INSTANCE.isEnabled() || client.player == null) {
            return true;
        }

        if (client.player.isPassenger() || client.player.isInWater() || client.player.isUnderWater()) {
            return true;
        }

        if (packet instanceof ServerboundPlayerCommandPacket command) {
            return command.getAction() != ServerboundPlayerCommandPacket.Action.START_SPRINTING;
        }

        if (packet instanceof ServerboundMovePlayerPacket movePacket) {
            if (ignoreNextMovePacket) {
                ignoreNextMovePacket = false;
                return true;
            }

            if (client.player.onGround() && client.player.fallDistance <= 0.0F) {
                ((PlayerMoveC2SPacketAccessor) movePacket).setOnGround(false);
            }
        }

        return true;
    }
}
