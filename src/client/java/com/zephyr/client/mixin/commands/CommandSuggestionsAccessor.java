package com.zephyr.client.mixin.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    void setCommandUsageWidth(int width);

    @Accessor("keepSuggestions")
    boolean getKeepSuggestions();
}
