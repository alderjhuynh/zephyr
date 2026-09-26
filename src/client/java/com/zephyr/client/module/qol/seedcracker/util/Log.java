package com.zephyr.client.module.qol.seedcracker.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import java.util.regex.Pattern;

/**
 * Chat-based logging helper for the seedcracker.
 *
 * <p>Messages are sent as system chat components on the main thread, with translation keys resolved
 * through the locale. Seeds and dungeon info are printed as click-to-copy square-bracketed
 * components.
 */
public class Log {

    /**
     * Sends a plain debug message.
     *
     * @param message the message text
     */
    public static void debug(String message) {
        sendMessage(Component.literal(message));
    }

    /**
     * Sends a formatted, green warn message.
     *
     * @param translateKey the translation key, with optional format placeholders
     * @param args the format arguments
     */
    public static void warn(String translateKey, Object... args) {
        String message = translate(translateKey).formatted(args);

        sendMessage(Component.literal(message).withStyle(ChatFormatting.GREEN));
    }

    /**
     * Sends a green warn message without arguments.
     *
     * @param translateKey the translation key
     */
    public static void warn(String translateKey) {
        warn(translateKey, new Object[]{});
    }

    /**
     * Sends a red error message.
     *
     * @param translateKey the translation key
     */
    public static void error(String translateKey) {
        String message = translate(translateKey);

        sendMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    /**
     * Sends a seed value as a click-to-copy bracketed component, substituting any "${SEED}" marker
     * in the translated message.
     *
     * @param translateKey the translation key
     * @param seedValue the seed to print
     */
    public static void printSeed(String translateKey, long seedValue) {
        String message = translate(translateKey);
        String[] data = message.split(Pattern.quote("${SEED}"));
        String seed = String.valueOf(seedValue);
        Component text = ComponentUtils.wrapInSquareBrackets((Component.literal(seed)).withStyle(style -> style.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent.CopyToClipboard(seed)).withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.copy.click"))).withInsertion(seed)));


        MutableComponent text1 = Component.literal(data[0]).append(text);
        if (data.length > 1) {
            text1.append(Component.literal(data[1]));
        }
        sendMessage(text1);
    }

    /**
     * Sends a click-to-copy bracketed component with the given dungeon info text.
     *
     * @param message the info text
     */
    public static void printDungeonInfo(String message) {
        Component text = ComponentUtils.wrapInSquareBrackets((Component.literal(message)).withStyle(style -> style.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent.CopyToClipboard(message)).withHoverEvent(new HoverEvent.ShowText( Component.translatable("chat.copy.click"))).withInsertion(message)));

        sendMessage(text);
    }

    /**
     * @param translateKey the translation key
     * @return the localised text for the key
     */
    public static String translate(String translateKey) {
        return Language.getInstance().getOrDefault(translateKey);
    }

    private static void schedule(Runnable runnable) {
        Minecraft.getInstance().execute(runnable);
    }

    private static void sendMessage(Component component) {
        schedule(() -> Minecraft.getInstance().gui.chatListener().handleSystemMessage(component, false));
    }

}
