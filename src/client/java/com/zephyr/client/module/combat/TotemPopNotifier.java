package com.zephyr.client.module.combat;

import com.zephyr.client.commands.CommandManager;
import com.zephyr.client.configplusgui.hud.NotificationManager;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// Backport of 26.3's TotemPopNotifier. 1.21.1 differences: chat goes through
// CommandManager.sendMessage (client.gui.getChat().addMessage), and the HUD
// counter renders via GuiGraphics (hooked from ZephyrClient's
// HudRenderCallback) instead of the 26.x Hud/GuiGraphicsExtractor mixin.
public final class TotemPopNotifier extends Module {
    public static final TotemPopNotifier INSTANCE = new TotemPopNotifier();

    private static final int COUNTER_START_Y = 4;
    private static final int COUNTER_LINE_GAP = 10;
    private static final int COUNTER_COLOR = 0xFFFF5555;

    private final BooleanSetting chatMessage = new BooleanSetting("Chat Message", true);
    private final BooleanSetting toast = new BooleanSetting("Toast", true);
    private final BooleanSetting hudCounter = new BooleanSetting("HUD Counter", true);

    private final Map<UUID, PopInfo> pops = new LinkedHashMap<>();

    private TotemPopNotifier() {
        super("Totem Pop Notifier", "Notifies you when nearby players pop their totems", Category.COMBAT);
        addSetting(chatMessage);
        addSetting(toast);
        addSetting(hudCounter);
    }

    public static void onPop(Minecraft client, Entity entity) {
        INSTANCE.handlePop(client, entity);
    }

    private void handlePop(Minecraft client, Entity entity) {
        if (client == null || client.player == null || client.level == null) return;
        if (!(entity instanceof Player player)) return;
        if (player == client.player) return;

        String name = player.getName().getString();
        PopInfo info = pops.computeIfAbsent(player.getUUID(), id -> new PopInfo(name));
        info.name = name;
        info.count++;

        String message = name + " popped a totem (" + info.count + "x)";
        if (chatMessage.get()) {
            CommandManager.sendMessage("§c" + message);
        }
        if (toast.get()) {
            NotificationManager.notifyText(message, COUNTER_COLOR);
        }
    }

    public void render(GuiGraphics graphics, Font font, int screenWidth) {
        if (!isEnabled() || !hudCounter.get() || pops.isEmpty()) return;

        int y = COUNTER_START_Y;
        for (PopInfo info : pops.values()) {
            graphics.drawString(font, info.name + " x" + info.count, 4, y, COUNTER_COLOR, true);
            y += COUNTER_LINE_GAP;
        }
    }

    @Override
    protected void onDisable() {
        pops.clear();
    }

    private static final class PopInfo {
        private String name;
        private int count;

        private PopInfo(String name) {
            this.name = name;
        }
    }
}
