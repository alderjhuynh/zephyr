package com.zephyr.client.mixin.combat.Reach;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zephyr.client.module.combat.Reach;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    @Shadow
    public abstract Abilities getAbilities();

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @ModifyReturnValue(method = "blockInteractionRange", at = @At("RETURN"))
    private double modifyBlockInteractionRange(double original) {
        if (!Reach.INSTANCE.isEnabled()) return original;
        return Math.max(0, original + Reach.INSTANCE.blockReach.get());
    }

    @ModifyReturnValue(method = "entityInteractionRange", at = @At("RETURN"))
    private double modifyEntityInteractionRange(double original) {
        if (!Reach.INSTANCE.isEnabled()) return original;
        return Math.max(0, original + Reach.INSTANCE.entityReach.get());
    }
}
