package com.zephyr.client.mixin.qol.MouseTweaks;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

/**
 * Accessor for AbstractContainerScreen needed by MouseTweaks.
 * Exposes quick-craft state and helper methods.
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("isQuickCrafting")
    boolean zephyr$getIsQuickCrafting();

    @Accessor("isQuickCrafting")
    void zephyr$setIsQuickCrafting(boolean value);

    @Accessor("quickCraftingButton")
    int zephyr$getQuickCraftingButton();

    @Accessor("skipNextRelease")
    boolean zephyr$getSkipNextRelease();

    @Accessor("skipNextRelease")
    void zephyr$setSkipNextRelease(boolean value);

    @Accessor("quickCraftSlots")
    Set<Slot> zephyr$getQuickCraftSlots();

    @Invoker("getHoveredSlot")
    Slot zephyr$invokeGetHoveredSlot(double mouseX, double mouseY);

    @Invoker("slotClicked")
    void zephyr$invokeSlotClicked(Slot slot, int slotId, int mouseButton, ContainerInput input);
}
