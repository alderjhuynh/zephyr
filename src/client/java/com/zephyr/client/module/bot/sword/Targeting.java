package com.zephyr.client.module.bot.sword;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Targeting {
   private LivingEntity locked;
   private Vec3 lastSeen = Vec3.ZERO;
   private boolean hasLastSeen;

   public LivingEntity locked() {
      return this.locked;
   }

   public Vec3 lastSeen() {
      return this.lastSeen;
   }

   public boolean hasLastSeen() {
      return this.hasLastSeen;
   }

   public void targetNearest(Minecraft var1, double var2) {
      this.locked = findNearest(var1, var2);
      if (this.locked != null) {
         this.lastSeen = this.locked.position();
         this.hasLastSeen = true;
      }
   }

   public void clear() {
      this.locked = null;
      this.hasLastSeen = false;
   }

   public void steal(LivingEntity var1) {
      this.locked = var1;
      if (this.locked != null) {
         this.lastSeen = this.locked.position();
         this.hasLastSeen = true;
      }
   }

   public void tick(Minecraft var1, SenseState var2, BotConfig var3) {
      if (var1.player != null && var1.level != null) {
         if (this.locked != null && (!this.locked.isAlive() || this.locked.isRemoved() || var2.ticksSinceSeen > 100)) {
            this.locked = null;
         }

         if (this.locked == null && var3.autoTarget) {
            this.locked = findNearest(var1, var3.autoTargetRange);
         }

         var2.onTargetChanged(this.locked);
         if (this.locked != null && SenseState.hasLos(var1, var1.player.getEyePosition(), this.locked)) {
            this.lastSeen = this.locked.position();
            this.hasLastSeen = true;
         }
      } else {
         this.locked = null;
      }
   }

   static boolean isEngageable(LivingEntity var0) {
      if (var0 == null) {
         return false;
      }

      try {
         if (var0.hasGlowingTag()) {
            return false;
         }
      } catch (Exception var4) {
      }

      try {
         if (var0.entityTags().contains("opus.countdown")) {
            return false;
         }
      } catch (Exception var3) {
      }

      try {
         if (var0.isInvulnerable()) {
            return false;
         }
      } catch (Exception var2) {
      }

      return true;
   }

   private static LivingEntity findNearest(Minecraft var0, double var1) {
      if (var0.player != null && var0.level != null) {
         LivingEntity var3 = null;
         double var4 = Double.MAX_VALUE;

         for (Entity var7 : var0.level.entitiesForRendering()) {
            if (var7 instanceof LivingEntity var8 && var8 != var0.player && var8.isAlive() && !(var8 instanceof ArmorStand) && isEngageable(var8)) {
               double var9 = var8.distanceTo(var0.player);
               if (!(var9 > var1) && SenseState.hasLos(var0, var0.player.getEyePosition(), var8)) {
                  double var11 = var9 + (var8 instanceof Player ? -1000.0 : 0.0);
                  if (var11 < var4) {
                     var4 = var11;
                     var3 = var8;
                  }
               }
            }
         }

         return var3;
      } else {
         return null;
      }
   }
}
