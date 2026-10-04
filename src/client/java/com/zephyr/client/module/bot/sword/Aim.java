package com.zephyr.client.module.bot.sword;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Aim {
   public static final double LEAD_TICKS = 0.0;
   public static final double LEAD_MAX = 0.0;
   private final Random random = new Random();
   private float currentYaw;
   private float currentPitch;
   private boolean initialized;
   public float wantYaw;
   public float wantPitch;

   public void snap(float var1, float var2) {
      this.currentYaw = var1;
      this.currentPitch = var2;
      this.initialized = true;
   }

   public void compute(Minecraft var1, SenseState var2, BotConfig var3) {
      if (var1.player != null && var2.target != null) {
         if (!this.initialized) {
            this.snap(var1.player.getYRot(), var1.player.getXRot());
         }

         Vec3 var4 = var2.eyePos;
         Vec3 var5 = var2.targetPos;
         Vec3 var6 = var2.leadFeet(0.0, 0.0);
         if (var6 != null) {
            var5 = var6;
         }

         double var7 = var2.targetWidth / 2.0;
         double var9 = clamp(var4.x, var5.x - var7 + 0.06, var5.x + var7 - 0.06);
         double var11 = clamp(var4.z, var5.z - var7 + 0.06, var5.z + var7 - 0.06);
         if (Math.abs(var4.x - var5.x) < 0.05 && Math.abs(var4.z - var5.z) < 0.05) {
            var9 = var5.x;
            var11 = var5.z;
         }

         double var13 = clamp(var4.y, var5.y + 0.06, var5.y + var2.targetHeight - 0.06);
         double var15 = var9 - var4.x;
         double var17 = var13 - var4.y;
         double var19 = var11 - var4.z;
         double var21 = Math.sqrt(var15 * var15 + var19 * var19);
         float var23 = (float)Math.toDegrees(Math.atan2(-var15, var19));
         float var24 = (float)Math.toDegrees(Math.atan2(-var17, var21));
         double var25 = var3.skill.aimErrDeg;
         if (var25 > 0.0) {
            var23 = (float)(var23 + (this.random.nextDouble() * 2.0 - 1.0) * var25);
            var24 = (float)(var24 + (this.random.nextDouble() * 2.0 - 1.0) * var25 * 0.6);
         }

         this.wantYaw = var23;
         this.wantPitch = Mth.clamp(var24, -90.0F, 90.0F);
      }
   }

   public float[] step(BotConfig var1) {
      double var2 = var1.skill.turnDegPerTick;
      this.currentYaw = approachAngle(this.currentYaw, this.wantYaw, var2);
      this.currentPitch = approach(this.currentPitch, this.wantPitch, var2);
      this.currentPitch = Mth.clamp(this.currentPitch, -90.0F, 90.0F);
      return new float[]{this.currentYaw, this.currentPitch};
   }

   public float[] predictStep(BotConfig var1) {
      double var2 = var1.skill.turnDegPerTick;
      float var4 = approachAngle(this.currentYaw, this.wantYaw, var2);
      float var5 = Mth.clamp(approach(this.currentPitch, this.wantPitch, var2), -90.0F, 90.0F);
      return new float[]{var4, var5};
   }

   public void applyTo(Minecraft var1, float var2, float var3) {
      if (var1.player != null) {
         this.currentYaw = Mth.wrapDegrees(var2);
         this.currentPitch = Mth.clamp(var3, -90.0F, 90.0F);
         var1.player.setYRot(this.currentYaw);
         var1.player.setXRot(this.currentPitch);
      }
   }

   private static float approach(float var0, float var1, double var2) {
      double var4 = var1 - var0;
      if (var4 > var2) {
         var4 = var2;
      } else if (var4 < -var2) {
         var4 = -var2;
      }

      return (float)(var0 + var4);
   }

   private static float approachAngle(float var0, float var1, double var2) {
      double var4 = Mth.wrapDegrees(var1 - var0);
      if (var4 > var2) {
         var4 = var2;
      } else if (var4 < -var2) {
         var4 = -var2;
      }

      return Mth.wrapDegrees((float)(var0 + var4));
   }

   private static double clamp(double var0, double var2, double var4) {
      return var2 > var4 ? (var2 + var4) / 2.0 : Math.max(var2, Math.min(var4, var0));
   }

   static double rayBox(Vec3 var0, float var1, float var2, Vec3 var3, double var4, double var6) {
      double var8 = Math.toRadians(var1);
      double var10 = Math.toRadians(var2);
      double var12 = -Math.sin(var8) * Math.cos(var10);
      double var14 = -Math.sin(var10);
      double var16 = Math.cos(var8) * Math.cos(var10);
      double var18 = var4 / 2.0;
      double var20 = 0.0;
      double var22 = Double.POSITIVE_INFINITY;
      double[] var24 = new double[]{var0.x, var0.y, var0.z};
      double[] var25 = new double[]{var12, var14, var16};
      double[] var26 = new double[]{var3.x - var18, var3.y, var3.z - var18};
      double[] var27 = new double[]{var3.x + var18, var3.y + var6, var3.z + var18};

      for (int var28 = 0; var28 < 3; var28++) {
         if (Math.abs(var25[var28]) < 1.0E-12) {
            if (var24[var28] < var26[var28] || var24[var28] > var27[var28]) {
               return Double.POSITIVE_INFINITY;
            }
         } else {
            double var29 = (var26[var28] - var24[var28]) / var25[var28];
            double var31 = (var27[var28] - var24[var28]) / var25[var28];
            if (var29 > var31) {
               double var33 = var29;
               var29 = var31;
               var31 = var33;
            }

            if (var29 > var20) {
               var20 = var29;
            }

            if (var31 < var22) {
               var22 = var31;
            }

            if (var20 > var22) {
               return Double.POSITIVE_INFINITY;
            }
         }
      }

      return var20;
   }
}
