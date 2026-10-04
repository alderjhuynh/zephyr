package com.zephyr.client.module.bot.sword;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

final class BowBrain {
   private int bowStartAge = -100000;
   private boolean drawing;

   boolean tick(Minecraft var1, SenseState var2, BotConfig var3, Aim var4, Intents var5, int var6) {
      var5.clear();
      if (var3.allowBow && var1.player != null && var2.target != null && var2.targetVisible) {
         boolean var7 = var2.target.isUsingItem() && var2.hdist >= var3.punishEaterDist;
         boolean var8 = var2.hdist >= var3.bowMinDist || var7;
         boolean var9 = var2.reachOp <= 4.5;
         boolean var10 = var2.hdist > var3.bowMaxDist;
         if ((!var8 || var9 || var10) && !this.drawing) {
            return false;
         }

         if (!var9 && var6 - this.bowStartAge < 40) {
            int var11 = RecoverBrain.findBowSlot(var1);
            if (var11 >= 0 && RecoverBrain.hasArrows(var1)) {
               if (!this.drawing) {
                  this.drawing = true;
                  this.bowStartAge = var6;
                  var1.player.getInventory().setSelectedSlot(var11);
               }

               var5.use = true;
               this.aimAt(var1, var2, var4);
               return true;
            } else {
               this.stop(var1);
               return false;
            }
         } else {
            this.stop(var1);
            return false;
         }
      } else {
         this.stop(var1);
         return false;
      }
   }

   private void aimAt(Minecraft var1, SenseState var2, Aim var3) {
      Vec3 var4 = var2.eyePos;
      Vec3 var5 = var2.targetPos;
      Vec3 var6 = new Vec3(var5.x, var5.y + var2.targetHeight * 0.6, var5.z);
      Vec3 var7 = var2.targetVelEma == null ? Vec3.ZERO : var2.targetVelEma;
      int var8 = solveFlightTicks(var4, var6, var7);
      Vec3 var9 = var6.add(var7.x * var8, var7.y * var8 * 0.5, var7.z * var8);
      double var10 = var9.x - var4.x;
      double var12 = var9.y - var4.y + arrowDrop(var8);
      double var14 = var9.z - var4.z;
      double var16 = Math.sqrt(var10 * var10 + var14 * var14);
      var3.wantYaw = (float)Math.toDegrees(Math.atan2(-var10, var14));
      var3.wantPitch = (float)Math.toDegrees(Math.atan2(-var12, var16));
   }

   private static int solveFlightTicks(Vec3 var0, Vec3 var1, Vec3 var2) {
      int var3 = 1;
      int var4 = 90;

      for (int var5 = 0; var5 < 7; var5++) {
         int var6 = (var3 + var4) / 2;
         double var7 = var1.y + var2.y * var6 * 0.5;
         double var9 = var7 - var0.y;
         if (var9 > maxRise(var6)) {
            var3 = var6;
         } else {
            var4 = var6;
         }

         if (var4 - var3 <= 1) {
            break;
         }
      }

      return Math.max(1, (var3 + var4) / 2);
   }

   private static double flightDist(int var0) {
      return 3.0 * (1.0 - Math.pow(0.99, var0)) / 0.01;
   }

   private static double maxRise(int var0) {
      return flightDist(var0);
   }

   private static double arrowDrop(int var0) {
      return 0.05 * var0 * (var0 - 1) / 2.0;
   }

   boolean drawing() {
      return this.drawing;
   }

   void stop(Minecraft var1) {
      this.drawing = false;
      this.bowStartAge = -100000;
      RecoverBrain.selectWeapon(var1);
   }
}
