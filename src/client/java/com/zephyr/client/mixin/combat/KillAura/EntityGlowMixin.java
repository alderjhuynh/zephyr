package com.zephyr.client.mixin.combat.KillAura;

import com.zephyr.client.module.combat.KillAura.KillAura;
import com.zephyr.client.module.combat.KillAura.TargetManager;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowMixin {
    @Shadow
    @Final
    private EntityType<?> type;

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void glow(CallbackInfoReturnable<Boolean> cir) {
        if (!KillAura.INSTANCE.isEnabled() || TargetManager.getTarget() == null) return;
        if ((Object) this instanceof LivingEntity) {
            cir.setReturnValue(true);
        }
    }
}
