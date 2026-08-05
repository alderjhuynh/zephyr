package com.zephyr.client.module.movement;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.mixin.movement.NoFall.PlayerMoveC2SPacketAccessor;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public final class NoFall extends Module {
    public static final NoFall INSTANCE = new NoFall();

    private NoFall() {
        super("No Fall", "Prevents fall damage packets", Category.MOVEMENT);
    }

    public static void onSendPacket(ServerboundMovePlayerPacket packet) {
        if (INSTANCE.isEnabled()) {
            ((PlayerMoveC2SPacketAccessor) packet).setOnGround(true);
        }
    }
}
