package com.zephyr.client.module.bot.sword;

import java.util.Random;
import net.minecraft.world.phys.Vec3;

final class MeleeBrain {
   private static final int STRAFE_FLIP_TICKS = 14;
   private static final double CONFIRM_REACH = 3.0;
   private static final double GHOST_SANITY = 4.2;
   private static final double INREACH_RANGE = 3.0;
   private static final double COMMIT_RAY = 2.999;
   private static final double COMMIT_REACH = 2.7;
   private static final double COMMIT_PICK = 3.0;
   private static final double FULL_SOON_STR = 0.96;
   private static final double FOE_KB_SUPPRESS_RANGE = 6.0;
   private static final int COMBO_TICKS = 4;
   private static final int HOP_QUIET_TICKS = 16;
   private static final double HUG_RANGE = 1.8;
   private static final double TRANSIT_RANGE = 2.5;
   private static final double TRANSIT_JUMP = 4.5;
   private static final double PICK_SANITY = 3.5;
   private static final double PUNISH_RANGE = 7.0;
   private static final double PUNISH_SPEED = 0.1;
   private static final double CRIT_JUMP_STR = 0.45;
   private final Random random = new Random();
   private int inreachTicks;
   private int thinkId;
   private int plan;
   private int planStep;
   private int planLen;
   private int strafeDir = 1;
   private int strafeFlip;
   private int lastJumpAge = -100000;
   private int lastGate = 6;
   private boolean punishing;
   private int lastFoeHurtTime;
   private int lastHitDealtAge = -100000;
   private int hygieneStreak;
   private boolean lastBrawl;
   private boolean lastLosing;
   private int flushAge = -100000;
   private boolean lastSwingWasFall;
   private int resetParity;
   private int lastBaitAge = -100000;
   private int lastSwingAge = -100000;

   void tick(SenseState var1, BotConfig var2, Aim var3, Intents var4, int var5, boolean var6, KitProfile var7) {
      var4.clear();
      this.lastGate = 6;
      this.punishing = false;
      if (var1.target != null && var1.targetVisible) {
         if (var2.skill == BotConfig.Skill.OPTIMAL) {
            float[] var38 = var3.predictStep(var2);
            double var39 = Aim.rayBox(var1.eyePos, var38[0], var38[1], var1.targetPos, var1.targetWidth, var1.targetHeight);
            boolean var40 = var1.hdist <= 6.0;
            var4.set(var40 && var1.sprinting ? 0 : 1, 0, var40 && var1.onGround, !var40);
            this.lastBrawl = var5 - var1.lastHurtAge <= 6;
            this.lastLosing = false;
            if (var39 > 3.0 || var1.reachMe > 3.0) {
               this.lastGate = 7;
            } else if (var1.strength < 0.92) {
               this.lastGate = 2;
            } else if (var1.targetHurtTime > 0) {
               this.lastGate = 3;
            } else if (var1.onGround || !(var1.fallDistance <= 0.0) && !var1.sprinting) {
               var4.attack = true;
               this.lastSwingAge = var5;
               this.lastGate = 0;
            } else {
               this.lastGate = 4;
            }

            return;
         }

         if (var1.targetHurtTime > this.lastFoeHurtTime && var5 - this.lastSwingAge <= 5) {
            this.lastHitDealtAge = var5;
         }

         this.lastFoeHurtTime = var1.targetHurtTime;
         boolean var8 = (var5 + this.thinkId) % Math.max(1, var2.skill.thinkTicks) == 0;
         if (var1.hdist <= 4.5 && var2.skill.thinkTicks <= 1) {
            var8 = true;
         }

         if (var1.hurtThisTick || this.planStep >= this.planLen) {
            var8 = true;
         }

         if (var1.targetVelEma != null && var1.targetVelEma.lengthSqr() > 0.0 && var1.target != null && !var1.target.onGround()) {
            var8 = true;
         }

         if (var8) {
            this.replan(var1, var2, var5);
         }

         this.planStep++;
         this.punishing = var1.hdist < 7.0 && var1.targetSpeed < 0.1;
         boolean var9 = var1.targetHealth <= 8.0;
         boolean var10 = var7 != null && var7.kbMode();
         boolean var11 = var5 - var1.lastHurtAge <= 6 && var1.hdist <= 4.5;
         this.lastBrawl = var11;
         boolean var12 = var11 && var1.targetHealth > 8.0 && var5 - this.lastHitDealtAge > 12;
         this.lastLosing = var12;
         float[] var13 = var3.predictStep(var2);
         Vec3 var14 = var1.leadFeet(0.0, 0.0);
         double var15 = var14 == null ? Double.POSITIVE_INFINITY : Aim.rayBox(var1.eyePos, var13[0], var13[1], var14, var1.targetWidth, var1.targetHeight);
         boolean var17 = var15 <= 3.0 && var1.reachMe <= 4.2;
         boolean var18 = var6 && var1.reachMe <= 3.5;
         boolean var19 = var17 || var18;
         double var20 = var1.targetWidth / 2.0;
         boolean var22 = var1.hdist < var20 + 0.1 && var1.eyePos.y > var1.targetPos.y - 0.2 && var1.eyePos.y < var1.targetPos.y + var1.targetHeight + 0.2;
         boolean var23 = !var22 && var15 <= 3.0 && var1.reachMe <= 3.0;
         boolean var24 = var6 && var1.reachMe <= 3.0;
         boolean var25 = var23 || var24;
         if (var1.reachMe <= 3.0) {
            this.inreachTicks++;
         } else {
            this.inreachTicks = 0;
         }

         boolean var26 = var1.foeKbLevel > 0 && var1.hdist <= 6.0;
         double var27 = 0.35 + Math.max(0.0, var1.radialVel);
         boolean var29 = var1.hdist - 12.0 * var27 >= 3.6;
         boolean var30 = !var19 && var1.hdist > 2.5;
         if (var30) {
            if (var26) {
               var4.set(1, 0, false, false);
            } else if (var1.hdist > 4.5) {
               var4.set(1, 0, false, true);
               if (var1.onGround && var29 && var1.hdist > 12.0) {
                  var4.jump = true;
               }

               if (var1.hdist > 7.0 && var1.radialVel < 0.3) {
                  this.strafeFlip++;
                  if (this.strafeFlip >= 20) {
                     this.strafeFlip = 0;
                     this.strafeDir *= -1;
                  }

                  var4.s = this.strafeDir;
               }
            } else if (var1.radialVel < -0.1) {
               var4.set(1, 0, false, true);
            } else if (var5 - this.lastHitDealtAge > 40 && var5 - var1.lastHurtAge > 40) {
               var4.set(1, 0, false, true);
            } else {
               var4.set(1, 0, false, false);
            }
         } else {
            int var31 = !this.punishing && !var9 && !(var1.hdist > 1.8) ? this.plan : 0;
            switch (var31) {
               case 1:
                  var4.set(0, 0, false, false);
                  break;
               default:
                  var4.set(1, 0, false, true);
            }

            if (!this.punishing
               && !var10
               && !var11
               && var1.onGround
               && this.lastSwingWasFall
               && var5 - this.lastHitDealtAge <= 2
               && !var1.target.onGround()
               && var1.hdist >= 1.0
               && var1.hdist <= 2.8
               && var5 - this.lastJumpAge >= 16) {
               var4.jump = true;
               this.lastJumpAge = var5;
            } else if (!this.punishing
               && !var10
               && !var11
               && var19
               && var1.onGround
               && var1.strength > 0.45
               && var1.strength < 0.95
               && var1.hdist >= 1.0
               && var1.hdist <= 2.8
               && var5 - this.lastJumpAge >= 16) {
               var4.jump = true;
               this.lastJumpAge = var5;
            }

            boolean var32 = var19 && this.inreachTicks >= 2 && var1.hdist <= 3.0 || var1.hdist <= 1.8;
            if (var32) {
               this.strafeFlip++;
               int var33 = Math.max(6, (int)Math.round(14.0 * var2.nnStrafeScale));
               if (this.strafeFlip >= var33) {
                  this.strafeFlip = 0;
                  this.strafeDir *= -1;
               }

               var4.s = this.strafeDir;
               boolean var34 = var5 - this.lastHitDealtAge <= 12 && var1.hdist <= 3.0;
               var4.sprint = var34;
            }

            if (var4.f == 0 && var4.s == 0) {
               var4.f = 1;
            }
         }

         int var41 = var5 - this.lastHitDealtAge;
         if (!var10 && !this.punishing && !var11 && !var12 && var1.onGround && var1.radialVel >= -0.1 && var1.targetHealth > 8.0 && var1.hdist <= 3.2) {
            if (var41 == 1) {
               var4.f = 0;
               var4.s = 0;
               var4.sprint = false;
            } else if (var41 >= 2 && var41 <= nnTapLen(var2) && var1.hdist <= 3.0) {
               var4.f = -1;
               var4.s = 0;
               var4.sprint = false;
            }
         }

         boolean var42 = var1.target != null && !var1.target.onGround();
         boolean var43 = opponentReadyIn(var1, var5) <= 1;
         if (!var10
            && !var11
            && !var12
            && var1.onGround
            && !var25
            && (var42 || var43)
            && var1.hdist >= 2.2
            && var1.hdist <= 3.5
            && var1.strength >= 0.9
            && var41 > 2
            && var5 - this.lastBaitAge >= 20) {
            var4.f = -1;
            var4.s = 0;
            var4.sprint = false;
            this.lastBaitAge = var5;
         }

         if (!var12 && !this.punishing && var1.targetHealth <= 8.0 && var5 - this.lastHitDealtAge <= 4 && var1.hdist <= 3.0) {
            var4.f = 1;
            var4.s = 0;
            var4.sprint = true;
         } else if (!var12 && this.punishing && var5 - this.lastHitDealtAge <= 4 && var1.hdist <= 3.0) {
            var4.f = 1;
            var4.s = 0;
            var4.sprint = true;
         }

         if (!var1.sprinting) {
            this.hygieneStreak = 0;
         }

         boolean var44 = false;
         if (var1.onGround && var4.f > 0 && var1.hurtThisTick) {
            this.resetParity++;
            boolean var35 = var1.hurtDmg >= 2.5;
            if (!var35 && this.resetParity % 2 != 0 && var1.hdist < 2.0) {
               var4.f = -1;
               var4.s = this.strafeDir;
               var4.sprint = false;
               var4.jump = false;
            } else {
               var4.set(1, 0, false, true);
               var4.jump = false;
               var44 = true;
            }
         } else if (var1.onGround && var4.f > 0 && !var12 && var1.justLanded && var5 - var1.lastHurtAge <= 5) {
            var4.set(1, 0, false, true);
            var4.jump = true;
            var44 = true;
         }

         boolean var45 = !var1.onGround && var5 - var1.lastHurtAge <= 1;
         if (var45 && !var44) {
            if (!(var1.hurtDmg >= 4.0) && !(var1.hdist >= 2.5)) {
               var4.f = -1;
               var4.s = this.strafeDir;
               var4.sprint = false;
            } else {
               var4.f = 1;
               var4.sprint = true;
            }

            var4.jump = false;
         }

         boolean var36 = var12 && var5 - var1.lastHurtAge >= 2 && (var1.foeKbLevel > 0 || var1.hdist > 3.5);
         this.lastLosing = var36;
         if (var36 && var1.hdist < 2.5) {
            var4.f = -1;
            var4.s = this.strafeDir;
            var4.sprint = false;
         }

         if (!var10 && !var1.onGround && var1.hdist <= 6.0) {
            var4.sprint = false;
            if (var1.sprinting) {
               var4.f = 0;
               var4.s = 0;
            }
         }

         if (!var25) {
            this.lastGate = 7;
         } else if (var5 - this.lastSwingAge <= 1) {
            this.lastGate = 4;
         } else if (this.inreachTicks <= var2.skill.reactTicks) {
            this.lastGate = 4;
         } else if (var1.strength <= 0.9) {
            this.lastGate = 2;
         } else if (var1.targetHurtTime > 0 && var5 - this.lastHitDealtAge <= 10) {
            this.lastGate = 3;
         } else {
            boolean var37 = var6 && var1.strength >= 0.999 && var1.reachMe <= 3.0;
            if (!var37
               && !var11
               && opponentReadyIn(var1, var5) >= 2
               && var1.target != null
               && !var1.target.onGround()
               && var1.targetVelEma != null
               && var1.targetVelEma.y < -0.15
               && var1.targetPos.y - var1.selfPos.y < 1.0) {
               this.lastGate = 4;
            } else if (!var37 && !var10 && !var11 && !var1.onGround && var1.fallDistance <= 0.0 && !var1.sprinting && opponentReadyIn(var1, var5) >= 2) {
               this.lastGate = 4;
            } else if (!var37
               && !var10
               && !var11
               && !var44
               && var1.sprinting
               && !var1.onGround
               && var1.fallDistance <= 0.0
               && var1.radialVel >= -0.1
               && opponentReadyIn(var1, var5) >= 2
               && this.hygieneStreak < 3) {
               this.hygieneStreak++;
               this.flushAge = var5;
               var4.set(0, 0, false, false);
               var4.jump = false;
               this.lastGate = 4;
            } else if (!var37 && !var10 && !var11 && !var1.sprinting && !var1.onGround && var5 - this.flushAge < 2 && opponentReadyIn(var1, var5) >= 2) {
               this.lastGate = 4;
            } else if (!var11 && opponentReadyIn(var1, var5) >= 2 && !var1.critOk && var1.strength < 1.0 && var1.strength >= 0.96) {
               this.lastGate = 4;
            } else {
               var4.attack = true;
               this.lastGate = 0;
               this.hygieneStreak = 0;
               this.lastSwingAge = var5;
               this.lastSwingWasFall = !var1.onGround && var1.fallDistance > 0.0;
               this.commitAttack(var4, var10 || var1.sprinting);
            }
         }
      } else {
         this.inreachTicks = 0;
         this.lastFoeHurtTime = 0;
         this.lastBrawl = false;
         this.lastLosing = false;
      }
   }

   private void commitAttack(Intents var1, boolean var2) {
      var1.f = 1;
      if (var2) {
         var1.s = 0;
         var1.sprint = true;
      } else {
         var1.sprint = false;
      }

      var1.jump = false;
   }

   void onAir(SenseState var1, Intents var2, int var3) {
      var2.jump = false;
   }

   private void replan(SenseState var1, BotConfig var2, int var3) {
      this.thinkId = this.thinkId + 1 & 0xFF;
      double var4 = var2.style.aggression / 100.0;
      double var6 = this.random.nextDouble();
      if (var1.hdist > 1.8) {
         this.plan = 0;
      } else if (var6 < var4) {
         this.plan = 0;
      } else {
         this.plan = 1;
      }

      if (var2.skill.mistakePerMille > 0 && this.random.nextInt(1000) < var2.skill.mistakePerMille) {
         this.plan = this.random.nextInt(2);
      }

      this.planStep = 0;
      this.planLen = Math.max(1, var2.skill.thinkTicks * 2);
   }

   int plan() {
      return this.plan;
   }

   int gate() {
      return this.lastGate;
   }

   boolean punishing() {
      return this.punishing;
   }

   boolean brawling() {
      return this.lastBrawl;
   }

   boolean losing() {
      return this.lastLosing;
   }

   static double nnCommitReach(BotConfig var0) {
      return Math.min(2.9, Math.max(2.4, var0.nnCommitReach));
   }

   static int nnTapLen(BotConfig var0) {
      return Math.max(1, Math.min(3, var0.nnTapLen));
   }

   static int opponentReadyIn(SenseState var0, int var1) {
      int var2 = Math.max(var0.lastHurtAge, var0.foeSwingAge);
      return Math.max(0, 11 - (var1 - var2));
   }

   void reset() {
      this.inreachTicks = 0;
      this.plan = 0;
      this.planStep = 0;
      this.planLen = 1;
      this.strafeDir = 1;
      this.strafeFlip = 0;
      this.lastGate = 6;
      this.punishing = false;
      this.lastFoeHurtTime = 0;
      this.lastHitDealtAge = -100000;
      this.hygieneStreak = 0;
      this.lastBrawl = false;
      this.lastLosing = false;
      this.lastSwingAge = -100000;
      this.flushAge = -100000;
      this.lastSwingWasFall = false;
      this.resetParity = 0;
      this.lastBaitAge = -100000;
   }
}
