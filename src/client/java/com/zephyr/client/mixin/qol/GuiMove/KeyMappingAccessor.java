package com.zephyr.client.mixin.qol.GuiMove;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor into {@link KeyMapping} exposing the private {@code key}
 * field.
 *
 * <p>Used by the GuiMove {@link KeyboardInputMixin} to read the physical key a
 * {@link KeyMapping} is bound to, so movement can be driven directly from the
 * keyboard while a GUI is open.
 */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    /**
     * Reads the {@link KeyMapping#getKey()} field via its backing field.
     *
     * @return the input key this mapping is bound to
     */
    @Accessor("key")
    InputConstants.Key zephyr$getKey();
}