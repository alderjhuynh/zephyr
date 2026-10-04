package com.zephyr.client.module.bot.sword;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class SenseState {
   public boolean valid;
   public boolean targetVisible;
   public int ticksSinceSeen;
   public int visibleTicks;
   public Vec3 selfPos;
   public Vec3 eyePos;
   public Vec3 selfVel;
   public boolean onGround;
   public double fallDistance;
   public double health;
   public double absorption;
   public int food;
   public boolean sprinting;
   public double strength;
   public boolean hurtThisTick;
   public LivingEntity target;
   public Vec3 targetPos;
   public Vec3 targetEye;
   public Vec3 targetVel;
   public Vec3 targetVelEma;
   public double targetWidth;
   public double targetHeight;
   public double targetEyeHeight;
   public double targetHealth;
   public int targetHurtTime;
   public boolean targetUsingItem;
   public double hdist;
   public double reachMe;
   public double rawReach;
   public double reachOp;
   public double radialVel;
   public double tangentialVel;
   public double targetSpeed;
   public boolean critOk;
   public int lastHurtAge = -100000;
   public int foeSwingAge = -100000;
   public boolean justLanded;
   public int foeKbLevel;
   public boolean prevGround = true;
   public double hurtDmg;
   private double lastHp = -1.0;
   private Vec3 lastTargetPos;
   private boolean wasFoeSwinging;
   private boolean wasGround = true;

   public void tick(Minecraft var1, LivingEntity var2, int var3) {
      this.valid = false;
      if (var1.player != null && var1.level != null) {
         this.valid = true;
         this.selfPos = var1.player.position();
         this.eyePos = var1.player.getEyePosition();
         this.selfVel = var1.player.getDeltaMovement();
         this.onGround = var1.player.onGround();
         this.prevGround = this.wasGround;
         this.justLanded = this.onGround && !this.wasGround;
         this.wasGround = this.onGround;
         this.fallDistance = var1.player.fallDistance;
         this.health = var1.player.getHealth();
         this.absorption = var1.player.getAbsorptionAmount();
         this.food = var1.player.getFoodData().getFoodLevel();
         this.sprinting = var1.player.isSprinting();

         try {
            this.strength = var1.player.getAttackStrengthScale(0.5F);
         } catch (NoSuchMethodError | NoClassDefFoundError var16) {
            this.strength = 1.0;
         }

         if (this.lastHp >= 0.0) {
            this.hurtThisTick = this.health + this.absorption < this.lastHp - 0.001;
            if (this.hurtThisTick) {
               this.lastHurtAge = var3;
               this.hurtDmg = this.lastHp - (this.health + this.absorption);
            }
         }

         this.lastHp = this.health + this.absorption;
         if (var2 != null && var2.isAlive()) {
            this.target = var2;
            boolean var4 = hasLos(var1, this.eyePos, this.target);
            if (!var4) {
               this.targetVisible = false;
               this.ticksSinceSeen++;
               this.visibleTicks = 0;
               this.lastTargetPos = null;
               this.targetVelEma = null;
               this.targetUsingItem = false;
               return;
            }

            this.targetVisible = true;
            this.ticksSinceSeen = 0;
            this.visibleTicks++;
            Vec3 var5 = this.target.position();
            if (this.lastTargetPos != null && this.target == var2) {
               this.targetVel = var5.subtract(this.lastTargetPos);
            } else {
               this.targetVel = Vec3.ZERO;
               this.targetVelEma = null;
            }

            if (this.targetVelEma == null) {
               this.targetVelEma = this.targetVel;
            } else {
               this.targetVelEma = this.targetVelEma.add(this.targetVel.subtract(this.targetVelEma).scale(0.3));
            }

            this.lastTargetPos = var5;
            this.targetPos = var5;
            this.targetWidth = this.target.getBbWidth();
            this.targetHeight = this.target.getBbHeight();
            this.targetEyeHeight = this.target.getEyeHeight();
            this.targetEye = var5.add(0.0, this.targetEyeHeight, 0.0);
            this.targetHealth = 20.0;
            this.targetHurtTime = this.target.hurtTime;
            boolean var6 = false;

            try {
               var6 = this.target.isSwinging();
            } catch (Exception var15) {
            }

            if (var6 && !this.wasFoeSwinging) {
               this.foeSwingAge = var3;
            }

            this.wasFoeSwinging = var6;
            boolean var7 = false;

            try {
               var7 = this.target.isUsingItem();
            } catch (Exception var14) {
            }

            this.targetUsingItem = var7;
            this.foeKbLevel = 0;
            double var8 = var5.x - this.selfPos.x;
            double var10 = var5.z - this.selfPos.z;
            this.hdist = Math.sqrt(var8 * var8 + var10 * var10);
            this.reachMe = eyeToBox(this.eyePos, var5, this.targetWidth, this.targetHeight);
            this.rawReach = this.reachMe;
            this.reachOp = eyeToBox(this.targetEye, this.selfPos, 0.6, 1.8);
            double var12 = Math.sqrt(this.targetVelEma.x * this.targetVelEma.x + this.targetVelEma.z * this.targetVelEma.z);
            this.targetSpeed = var12;
            if (this.hdist > 1.0E-6) {
               this.radialVel = -(this.targetVelEma.x * var8 + this.targetVelEma.z * var10) / this.hdist;
               this.tangentialVel = (this.targetVelEma.x * var10 - this.targetVelEma.z * var8) / this.hdist;
            } else {
               this.radialVel = 0.0;
               this.tangentialVel = 0.0;
            }

            this.critOk = this.fallDistance > 0.0 && !this.onGround && !this.sprinting && this.strength > 0.9;
         } else {
            this.target = null;
            this.targetVisible = false;
            this.ticksSinceSeen++;
            this.visibleTicks = 0;
         }
      }
   }

   public void clearTargetMemory() {
      this.lastTargetPos = null;
      this.targetVelEma = null;
      this.foeSwingAge = -100000;
      this.wasFoeSwinging = false;
   }

   static boolean hasLos(Minecraft var0, Vec3 var1, LivingEntity var2) {
      Vec3 var3 = var2.position().add(0.0, var2.getEyeHeight(), 0.0);
      Vec3 var4 = var3.subtract(var1);
      double var5 = Math.toRadians(var0.player.getYRot());
      double var7 = Math.toRadians(var0.player.getXRot());
      double var9 = -Math.sin(var5) * Math.cos(var7) * var4.x - Math.sin(var7) * var4.y + Math.cos(var5) * Math.cos(var7) * var4.z;
      double var11 = Math.cos(var5) * var4.x + Math.sin(var5) * var4.z;
      double var13 = Math.sin(var5) * Math.sin(var7) * var4.x + Math.cos(var7) * var4.y - Math.cos(var5) * Math.sin(var7) * var4.z;
      double var15 = Math.tan(Math.toRadians(((Integer)var0.options.fov().get()).intValue() / 2.0));
      double var17 = (double)var0.getWindow().getWidth() / Math.max(1, var0.getWindow().getHeight());
      if (!(var9 <= 0.0)
         && !(Math.abs(var11) > var9 * var15 * var17 + var2.getBbWidth() / 2.0F)
         && !(Math.abs(var13) > var9 * var15 + var2.getBbHeight() / 2.0F)) {
         ClipContext var19 = new ClipContext(var1, var3, Block.COLLIDER, Fluid.NONE, var0.player);
         BlockHitResult var20 = var0.level.clip(var19);
         return var20.getType() == Type.MISS;
      } else {
         return false;
      }
   }

   static double eyeToBox(Vec3 var0, Vec3 var1, double var2, double var4) {
      double var6 = var2 / 2.0;
      double var8 = clampAxis(var0.x, var1.x - var6, var1.x + var6);
      double var10 = clampAxis(var0.z, var1.z - var6, var1.z + var6);
      double var12 = clampAxis(var0.y, var1.y, var1.y + var4);
      double var14 = var0.x - var8;
      double var16 = var0.y - var12;
      double var18 = var0.z - var10;
      return Math.sqrt(var14 * var14 + var16 * var16 + var18 * var18);
   }

   private static double clampAxis(double var0, double var2, double var4) {
      return Math.max(var2, Math.min(var4, var0));
   }

   public Vec3 leadFeet(double var1, double var3) {
      if (this.targetPos == null) {
         return null;
      }

      if (this.targetVelEma != null && !(var1 <= 0.0)) {
         Vec3 var5 = this.targetVelEma.multiply(var1, var1, var1);
         double var6 = var5.length();
         if (var6 > var3 && var6 > 1.0E-9) {
            var5 = var5.scale(var3 / var6);
         }

         return this.targetPos.add(var5);
      } else {
         return this.targetPos;
      }
   }

   public void endTick() {
      this.hurtThisTick = false;
   }

   public void onTargetChanged(Entity var1) {
      if (this.target != var1) {
         this.lastTargetPos = null;
         this.targetVelEma = null;
         this.visibleTicks = 0;
         this.foeSwingAge = -100000;
         this.wasFoeSwinging = false;
      }
   }
}
