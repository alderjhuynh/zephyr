package com.zephyr.client.module.combat;

import com.zephyr.client.configplusgui.hud.NotificationManager;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Combat module that watches for nearby players popping their totem of undying. Each pop is
 * reported through the configured channels (a chat message, a toast, or a HUD counter) with a
 * running per-player total. Pops are detected by the TotemPopNotifier entity-event mixin and
 * the counter is drawn by the TotemPopNotifier HUD mixin.
 */
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

    /** Handles a totem pop reported by the entity-event packet mixin. */
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
            client.gui.hud.getChat().addClientSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
        }
        if (toast.get()) {
            NotificationManager.notifyText(message, COUNTER_COLOR);
        }
    }

    /** Draws the per-player pop counter at the top of the screen while enabled. */
    public void render(GuiGraphicsExtractor graphics, Font font, int screenWidth) {
        if (!isEnabled() || !hudCounter.get() || pops.isEmpty()) return;

        int y = COUNTER_START_Y;
        for (PopInfo info : pops.values()) {
            graphics.text(font, info.name + " x" + info.count, 4, y, COUNTER_COLOR, true);
            y += COUNTER_LINE_GAP;
        }
    }

    /** Clears the pop tracking when the module is disabled. */
    @Override
    protected void onDisable() {
        pops.clear();
    }

    /** Running total of totem pops for a single player. */
    private static final class PopInfo {
        private String name;
        private int count;

        private PopInfo(String name) {
            this.name = name;
        }
    }
}
