package com.zephyr.client.module.qol.shulkerboxtooltip;

import com.zephyr.client.module.qol.ShulkerBoxTooltip;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides information for item previews, such as the item stack and player that owns the
 * stack (if present).
 */
public record PreviewContext(ItemStack stack, @Nullable Player owner, PreviewConfiguration config,
                             @Nullable HolderLookup.Provider registryLookup) {

    /** @return a new builder starting from the given item stack */
    @NotNull
    public static Builder builder(ItemStack stack) {
        return new Builder(stack);
    }

    /**
     * A builder for creating {@link PreviewContext} instances.
     */
    public static class Builder {
        private final ItemStack stack;
        private Player owner;
        private HolderLookup.Provider registryLookup;

        private Builder(ItemStack stack) {
            this.stack = stack;
        }

        /** Sets the player who owns the stack, if any. */
        public Builder withOwner(@Nullable Player owner) {
            this.owner = owner;
            return this;
        }

        /** Sets an explicit registry lookup, overriding the owner's registry access. */
        public Builder withRegistryLookup(@Nullable HolderLookup.Provider registryLookup) {
            this.registryLookup = registryLookup;
            return this;
        }

        /**
         * Builds the context, deriving the registry lookup from the owner when not
         * supplied explicitly and filling in the current module configuration.
         *
         * @return the assembled {@link PreviewContext}
         */
        @NotNull
        public PreviewContext build() {
            if (this.registryLookup == null && this.owner != null) {
                this.registryLookup = this.owner.registryAccess();
            }
            return new PreviewContext(this.stack, this.owner, ShulkerBoxTooltip.INSTANCE.configuration(),
                    this.registryLookup);
        }
    }
}
