package com.zephyr.client.module.bot.sword;

import java.io.File;
import java.io.FileWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

public final class BotEngine {
   private final BotConfig config = BotConfig.defaults();
   private final Targeting targeting = new Targeting();
   private final SenseState sense = new SenseState();
   private final Aim aim = new Aim();
   private final MeleeBrain melee = new MeleeBrain();
   private final PursueBrain pursue = new PursueBrain();
   private final RecoverBrain recover = new RecoverBrain();
   private final BowBrain bow = new BowBrain();
   private final Motor motor = new Motor();
   private final Intents intents = new Intents();
   private final Intents scratch = new Intents();
   private final FightLogger fights = new FightLogger();
   private final KitProfile kit = new KitProfile();
   private final NnBrain nn = new NnBrain();
   private boolean enabled;
   private int age;
   private Player lastPlayer;
   private boolean wasSidelined = true;
   private FileWriter cloneOut;
   private int cloneWrites;

   public BotConfig config() {
      return this.config;
   }

   public boolean enabled() {
      return this.enabled;
   }

   public void setEnabled(Minecraft var1, boolean var2) {
      this.enabled = var2;
      if (!var2) {
         this.motor.releaseAll(var1);
         this.melee.reset();
         this.recover.reset();
         this.bow.stop(var1);
         this.fights.abort(var1);

         try {
            if (this.cloneOut != null) {
               this.cloneOut.flush();
            }
         } catch (Exception var4) {
         }
      } else if (var1 != null && var1.player != null) {
         this.aim.snap(var1.player.getYRot(), var1.player.getXRot());
         this.targeting.targetNearest(var1, this.config.autoTargetRange);
         this.sense.clearTargetMemory();
      }
   }

   public void retarget(Minecraft var1) {
      this.targeting.targetNearest(var1, this.config.autoTargetRange);
      this.sense.clearTargetMemory();
   }

   public String statusLine() {
      String var1 = this.sense.target == null
         ? "none"
         : this.sense.target.getName().getString() + String.format(" %.1fb %s", this.sense.hdist, this.sense.targetVisible ? "seen" : "hidden");
      return String.format("SwordBot %s skill=%s style=%s target=%s [%s]", this.enabled ? "ON" : "OFF", this.config.skill, this.config.style, var1, "zephyr-bot");
   }

   public void tick(Minecraft var1) {
      if (var1 != null && var1.player != null && var1.level != null && this.enabled) {
         if (!var1.player.isAlive() || var1.gui.screen() != null) {
            this.motor.releaseAll(var1);
            return;
         }

         this.age++;
         if (var1.player != this.lastPlayer) {
            this.lastPlayer = var1.player;
            this.aim.snap(var1.player.getYRot(), var1.player.getXRot());
            this.sense.clearTargetMemory();
         }

         this.targeting.tick(var1, this.sense, this.config);

         try {
            if (var1.crosshairPickEntity instanceof LivingEntity var2
               && var2 != this.targeting.locked()
               && var2.isAlive()
               && Targeting.isEngageable(var2)
               && !(var2 instanceof ArmorStand)) {
               LivingEntity var14 = this.targeting.locked();
               boolean var4 = var14 == null || !var14.isAlive() || var14.distanceTo(var1.player) > 8.0 || !this.sense.targetVisible || this.sense.reachMe > 3.5;
               if (var4 && var2.distanceTo(var1.player) <= this.config.autoTargetRange) {
                  this.targeting.steal(var2);
                  this.sense.onTargetChanged(var2);
                  this.sense.clearTargetMemory();
               }
            }
         } catch (Exception var12) {
         }

         this.sense.tick(var1, this.targeting.locked(), this.age);
         if (this.sense.valid) {
            if (this.kit.needsScan(this.age)) {
               this.kit.rescan(var1);
            }

            this.kit.refreshWield(var1);
            LivingEntity var13 = this.targeting.locked();
            boolean var15 = hasCountdownTag(var13) || hasGlowingTag(var13);
            if (this.sense.target != null && !var15 && !this.sense.target.isInvulnerable()) {
               if (this.wasSidelined) {
                  this.wasSidelined = false;
                  if (var1.player != null) {
                     this.aim.snap(var1.player.getYRot(), var1.player.getXRot());
                  }

                  this.sense.clearTargetMemory();
                  this.melee.reset();
               }

               boolean var16 = this.sense.targetVisible || this.sense.targetPos != null;
               if (var16) {
                  this.aim.compute(var1, this.sense, this.config);
               }

               boolean var5 = this.sense.target != null && var1.crosshairPickEntity == this.sense.target;
               int var6 = 6;
               String var7 = "M?";
               if (this.recover.tick(var1, this.sense, this.config, this.intents, this.age)) {
                  var7 = "R" + this.recover.mode();
               } else if (this.bow.tick(var1, this.sense, this.config, this.aim, this.intents, this.age)) {
                  var7 = "B";
               } else if (this.pursue.tick(this.sense, this.targeting, this.scratch)) {
                  var7 = "P";
                  this.intents.f = this.scratch.f;
                  this.intents.s = this.scratch.s;
                  this.intents.jump = this.scratch.jump;
                  this.intents.sprint = this.scratch.sprint;
                  this.intents.attack = false;
                  this.intents.use = false;
                  this.melee.tick(this.sense, this.config, this.aim, this.scratch, this.age, var5, this.kit);
                  var6 = this.melee.gate();
                  if (this.scratch.attack) {
                     this.intents.attack = true;
                  }
               } else {
                  if (!this.sense.targetVisible) {
                     this.intents.clear();
                     var16 = false;
                  } else if (!this.sense.onGround) {
                     var7 = "MA";
                     this.melee.tick(this.sense, this.config, this.aim, this.scratch, this.age, var5, this.kit);
                     var6 = this.melee.gate();
                     if (this.melee.punishing()) {
                        var7 = "MAP";
                     }

                     this.intents.f = this.scratch.f;
                     this.intents.s = this.scratch.s;
                     this.intents.jump = false;
                     this.intents.sprint = this.scratch.sprint;
                     this.intents.attack = this.scratch.attack;
                     this.intents.use = false;
                  } else {
                     this.melee.tick(this.sense, this.config, this.aim, this.intents, this.age, var5, this.kit);
                     var6 = this.melee.gate();
                     var7 = "M" + this.melee.plan() + (this.melee.punishing() ? "P" : "");
                  }

                  if (this.sense.targetVisible && this.sense.reachOp <= 4.5 && this.bow.drawing()) {
                     this.bow.stop(var1);
                  }
               }

               if (this.sense.target != null && !this.fights.tracks(this.sense.target)) {
                  this.kit.rescan(var1);
                  this.fights.begin(var1, this.sense.target, this.config, this.age, this.kit, "switched");

                  try {
                     this.nn.onFightBegin(var1.gameDirectory, this.config);
                  } catch (Exception var11) {
                  }
               }

               if (this.intents.attack && !this.kit.ensureWeaponForAttack(var1, this.sense, this.config)) {
                  this.intents.attack = false;
               }

               if (!this.intents.attack) {
                  this.kit.applyDoctrine(var1, this.sense, this.config, this.recover, this.bow, this.intents);
               }

               boolean var8 = this.intents.attack;
               this.intents.attack = this.motor.apply(var1, this.intents, this.aim, this.config, var16, this.sense.target);
               if (var8 && !this.intents.attack) {
                  var6 = 7;
               }

               if (this.intents.attack) {
                  this.fights.onSwing();
               }

               if (var6 == 6 && this.sense.target != null && this.sense.targetVisible) {
                  if (this.intents.attack) {
                     var6 = 0;
                  } else if (this.intents.use) {
                     var6 = 5;
                  } else if (this.sense.strength <= 0.9) {
                     var6 = 2;
                  } else if (this.sense.targetHurtTime > 0) {
                     var6 = 3;
                  } else {
                     var6 = 4;
                  }
               }

               this.fights.onTick(var1, this.sense, this.intents, var6, var7, this.kit.wieldKb, this.melee.brawling(), this.melee.losing());

               try {
                  FightLogger.FightResult var9 = this.fights.pollResult();
                  if (var9 != null && this.nn.onFightEnd(var1.gameDirectory, this.config, var9)) {
                     this.shutdownAfterStreak(var1);
                  }
               } catch (Exception var10) {
               }

               if (this.config.cloneDump && this.sense.valid && this.sense.targetVisible && this.sense.target != null) {
                  this.cloneDump(var1, this.age);
               }

               this.sense.endTick();
            } else {
               if (var15 && var13 != null && this.fights.tracks(var13)) {
                  this.fights.closeRound(var1);
               }

               this.wasSidelined = true;
               this.motor.releaseAll(var1);
               this.bow.stop(var1);
               this.sense.endTick();
            }
         }
      }
   }

   private void shutdownAfterStreak(Minecraft var1) {
      try {
         if (var1.player != null) {
            var1.player.sendSystemMessage(Component.literal("[Zephyr] SwordBot: 10 consecutive wins — stopping to avoid overfit. " + this.nn.statusLine()));
         }
      } catch (Exception var4) {
      }

      this.setEnabled(var1, false);
   }

   public void toggleNn(Minecraft var1) {
      this.config.nnEnabled = !this.config.nnEnabled;
      if (var1 != null && var1.player != null) {
         var1.player
            .sendSystemMessage(
               Component.literal("[Zephyr] SwordBot nn " + (this.config.nnEnabled ? "ON" : "OFF") + " " + this.nn.statusLine() + " style=" + this.config.style)
            );
      }
   }

   public void noteManualStyle() {
      this.nn.noteManualStyle();
   }

   private void cloneDump(Minecraft var1, int var2) {
      try {
         if (this.cloneOut == null) {
            File var3 = new File(var1.gameDirectory, "zephyr-telemetry/swordbot");
            var3.mkdirs();
            File var4 = new File(var3, "clone.csv");
            boolean var5 = !var4.isFile() || var4.length() == 0L;
            this.cloneOut = new FileWriter(var4, true);
            if (var5) {
               this.cloneOut
                  .write(
                     "hdist,reachMe,reachOp,strength,hp,food,onGround,fall,hurt,recent,foeReady,radial,tang,speed,foeAir,foeUse,sprinting,foeHp,f,s,jump,sprint,attack,use,age,selfX,selfY,selfZ,targetX,targetY,targetZ,selfVx,selfVy,selfVz\n"
                  );
            }
         }

         int var9 = Math.max(this.sense.lastHurtAge, this.sense.foeSwingAge);
         double var10 = Math.max(0, 11 - (var2 - var9)) / 11.0;
         double var6 = this.sense.targetHealth / 20.0;
         this.cloneOut
            .write(
               clamp01(this.sense.hdist / 5.0, 2.0)
                  + ","
                  + clamp01(this.sense.reachMe / 3.0, 2.0)
                  + ","
                  + clamp01(this.sense.reachOp / 4.5, 2.0)
                  + ","
                  + clamp01(this.sense.strength, 1.0)
                  + ","
                  + clamp01((this.sense.health + this.sense.absorption) / 20.0, 1.5)
                  + ","
                  + clamp01(this.sense.food / 20.0, 1.0)
                  + ","
                  + (this.sense.onGround ? 1 : 0)
                  + ","
                  + clamp01(this.sense.fallDistance / 3.0, 1.0)
                  + ","
                  + clamp01(this.sense.targetHurtTime / 10.0, 1.0)
                  + ","
                  + (var2 - this.sense.lastHurtAge <= 6 ? 1 : 0)
                  + ","
                  + clamp01(var10, 1.0)
                  + ","
                  + clampSym(this.sense.radialVel * 3.0)
                  + ","
                  + clampSym(this.sense.tangentialVel * 3.0)
                  + ","
                  + clamp01(this.sense.targetSpeed * 3.0, 1.0)
                  + ","
                  + (!this.sense.target.onGround() ? 1 : 0)
                  + ","
                  + (this.sense.targetUsingItem ? 1 : 0)
                  + ","
                  + (this.sense.sprinting ? 1 : 0)
                  + ","
                  + clamp01(var6, 1.5)
                  + ","
                  + this.intents.f
                  + ","
                  + this.intents.s
                  + ","
                  + (this.intents.jump ? 1 : 0)
                  + ","
                  + (this.intents.sprint ? 1 : 0)
                  + ","
                  + (this.intents.attack ? 1 : 0)
                  + ","
                  + (this.intents.use ? 1 : 0)
                  + ","
                  + var2
                  + ","
                  + this.sense.selfPos.x
                  + ","
                  + this.sense.selfPos.y
                  + ","
                  + this.sense.selfPos.z
                  + ","
                  + this.sense.targetPos.x
                  + ","
                  + this.sense.targetPos.y
                  + ","
                  + this.sense.targetPos.z
                  + ","
                  + this.sense.selfVel.x
                  + ","
                  + this.sense.selfVel.y
                  + ","
                  + this.sense.selfVel.z
                  + "\n"
            );
         if (++this.cloneWrites % 100 == 0) {
            this.cloneOut.flush();
         }
      } catch (Exception var8) {
      }
   }

   private static double clamp01(double var0, double var2) {
      return Math.max(0.0, Math.min(var2, var0));
   }

   private static double clampSym(double var0) {
      return Math.max(-1.0, Math.min(1.0, var0));
   }

   private static boolean hasCountdownTag(LivingEntity var0) {
      if (var0 == null) {
         return false;
      }

      try {
         return var0.entityTags().contains("opus.countdown");
      } catch (Exception var2) {
         return false;
      }
   }

   private static boolean hasGlowingTag(LivingEntity var0) {
      if (var0 == null) {
         return false;
      }

      try {
         return var0.hasGlowingTag();
      } catch (Exception var2) {
         return false;
      }
   }
}
