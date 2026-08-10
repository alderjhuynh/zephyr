package com.zephyr.client.mixin.commands;

import com.zephyr.client.commands.Command;
import com.zephyr.client.commands.CommandManager;
import com.zephyr.client.commands.CommandPrefixHandler;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {

    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    private void zephyr$completeCustomCommands(CallbackInfo ci) {
        CommandSuggestionsAccessor self = (CommandSuggestionsAccessor) (Object) this;
        EditBox input = self.getInput();
        String text = input.getValue();
        String prefix = CommandPrefixHandler.currentPrefix();
        if (prefix == null || prefix.isEmpty() || !text.startsWith(prefix)) {
            return;
        }

        int cursor = input.getCursorPosition();
        if (self.getKeepSuggestions()) {
            self.getCommandUsage().clear();
            ci.cancel();
            return;
        }

        String upToCursor = text.substring(0, cursor);
        int wordStart = cursor;
        while (wordStart > 0 && !Character.isWhitespace(upToCursor.charAt(wordStart - 1))) {
            wordStart--;
        }
        int rangeStart = Math.min(Math.max(wordStart, prefix.length()), cursor);

        Suggestions suggestions = CommandManager.suggest(text, prefix, cursor);
        self.setPendingSuggestions(CompletableFuture.completedFuture(suggestions));

        self.getCommandUsage().clear();
        Command command = CommandManager.resolve(text, prefix);
        if (command != null) {
            FormattedCharSequence usage = Component
                    .literal(command.getName() + " - " + command.getDescription())
                    .getVisualOrderText();
            self.getCommandUsage().add(usage);
            int width = self.getCommandUsage().stream()
                    .mapToInt(line -> self.getMinecraft().font.width(line))
                    .max()
                    .orElse(0) + 2;
            self.setCommandUsageWidth(width);
            int minX = input.getScreenX(0);
            int maxX = minX + input.getInnerWidth() - width;
            self.setCommandUsagePosition(Mth.clamp(input.getScreenX(rangeStart), minX, maxX));
        }

        if (self.getMinecraft().options.autoSuggestions().get()) {
            ((CommandSuggestions) (Object) this).showSuggestions(false);
        }
        ci.cancel();
    }
}
