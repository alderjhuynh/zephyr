package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.phys.Vec3;

/**
 * Massive-damage spear module. While enabled and a spear is being charged, sends a
 * blink of two spoofed position packets each tick: first jumping the server-side
 * player {@link #blink} blocks behind the look direction, then back onto the local
 * position. The last packet the server processes sets its "known movement" to a large
 * forward delta, and {@link KineticWeapon} stab damage scales with that measured speed
 * ({@code getMotion = getKnownSpeed().scale(20)}), producing huge hits while the local
 * player never moves. The damage window only opens after the weapon's own delay ticks
 * have elapsed, which is read straight off the item component.
 */
public final class SpearDamage extends Module {
    public static final SpearDamage INSTANCE = new SpearDamage();

    private final NumberSetting blink = new NumberSetting("Blink", 9D, 1D, 9D, 1D);

    private SpearDamage() {
        super("Spear Damage", "Spoofs speed for massive spear stabs without moving", Category.COMBAT);
        addSetting(blink);
    }

    /**
     * Sends the blink spoof each tick while a spear is charged and its damage window is
     * open. The local player stays put; only the packets claim movement.
     *
     * @param client the Minecraft client instance
     */
    @Override
    public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.getConnection() == null) return;
        if (!player.isUsingItem()) return;

        ItemStack weapon = player.getUseItem();
        KineticWeapon kinetic = weapon.get(DataComponents.KINETIC_WEAPON);
        if (kinetic == null) return;

        int ticksUsed = weapon.getUseDuration(player) - player.getUseItemRemainingTicks();
        if (ticksUsed < kinetic.delayTicks()) return;

        Vec3 look = player.getLookAngle();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        if (horizontal < 1.0E-6D) return;

        Vec3 flatLook = new Vec3(look.x / horizontal, 0.0D, look.z / horizontal);
        Vec3 back = player.position().subtract(flatLook.scale(blink.get()));
        boolean onGround = player.onGround();

        sendPos(back, onGround);
        sendPos(player.position(), onGround);
    }

    private static void sendPos(Vec3 pos, boolean onGround) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) return;
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, onGround, false));
    }
}
