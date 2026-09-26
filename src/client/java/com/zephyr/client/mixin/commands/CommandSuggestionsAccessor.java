package com.zephyr.client.mixin.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Exposes the private fields and methods of {@link CommandSuggestions} that
 * {@link CommandSuggestionsMixin} needs in order to present Zephyr's own commands and
 * arguments in the chat box's tab-completion UI.
 */
@Mixin(CommandSuggestions.class)
public interface CommandSuggestionsAccessor {

    @Accessor("input")
    EditBox getInput();

    @Accessor("minecraft")
    Minecraft getMinecraft();

    @Accessor("pendingSuggestions")
    void setPendingSuggestions(CompletableFuture<Suggestions> pendingSuggestions);

    @Accessor("commandUsage")
    List<FormattedCharSequence> getCommandUsage();

    @Accessor("commandUsagePosition")
    void setCommandUsagePosition(int position);

    @Accessor("commandUsageWidth")
    int getCommandUsageWidth();

    @Accessor("currentParseIsCommand")
    void setCurrentParseIsCommand(boolean value);

    @Accessor("currentParseIsMessage")
    void setCurrentParseIsMessage(boolean value);

    @Accessor("keepSuggestions")
    boolean getKeepSuggestions();

    @Invoker("recomputeUsageBoxWidth")
    void invokeRecomputeUsageBoxWidth();
}
