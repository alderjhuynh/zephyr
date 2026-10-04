package com.zephyr.client.module.bot.sword;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class FightLogger {
   private static final int TRACE_EVERY = 10;
   private static final int TRACE_KEEP = 60;
   private static final int TIMEOUT_TICKS = 3600;
   private boolean open;
   private int idx;
   private long t0ms;
   private int ticks;
   private int swings;
   private int hits;
   private double dmgDealt;
   private int taken;
   private double dmgTaken;
   private int eatTicks;
   private int eatSessions;
   private boolean wasEating;
   private int rangeTicks;
   private double rangeSum;
   private int rangeCount;
   private int gapsStart;
   private int foodStart;
   private String foe = "?";
   private String weapon = "?";
   private String skill = "?";
   private String style = "?";
   private double lastFoeHp = -1.0;
   private double lastMyHp = -1.0;
   private double lastReach = -1.0;
   private int lastHurt = -1;
   private int sinceTrace;
   private LivingEntity tracked;
   private final List<String> trace = new ArrayList<>();
   private final int[] gates = new int[8];
   private int airTicks;
   private int strafeTicks;
   private int sprintTicks;
   private int readyTicks;
   private int readyIdleTicks;
   private int airSwings;
   private int fallSwings;
   private int kbSwings;
   private int sprSwings;
   private int brawlTicks;
   private int retreatTicks;
   private String kit = "?";
   private int kbLvl;
   private FightLogger.FightResult pendingResult;

   boolean begin(Minecraft var1, LivingEntity var2, BotConfig var3, int var4, KitProfile var5, String var6) {
      this.close(var1, var6);

      try {
         if (var1 != null && var1.player != null && var1.player.isAlive() && !(var1.player.getHealth() <= 0.0F) && var2 != null && var2.isAlive()) {
            this.open = true;
            this.idx++;
            this.t0ms = System.currentTimeMillis();
            this.ticks = 0;
            this.swings = 0;
            this.hits = 0;
            this.dmgDealt = 0.0;
            this.taken = 0;
            this.dmgTaken = 0.0;
            this.eatTicks = 0;
            this.eatSessions = 0;
            this.wasEating = false;
            this.rangeTicks = 0;
            this.rangeSum = 0.0;
            this.rangeCount = 0;
            this.gapsStart = countGaps(var1);
            this.foodStart = countFood(var1);
            this.tracked = var2;
            this.foe = var2.getName().getString();
            this.weapon = heldId(var1);
            this.skill = var3.skill.name();
            this.style = var3.style.name();
            this.lastFoeHp = Double.NaN;
            this.lastMyHp = myHp(var1);
            this.lastReach = -1.0;
            this.lastHurt = var1.player.hurtTime;
            this.sinceTrace = 0;
            this.trace.clear();
            Arrays.fill(this.gates, 0);
            this.airTicks = 0;
            this.strafeTicks = 0;
            this.sprintTicks = 0;
            this.readyTicks = 0;
            this.readyIdleTicks = 0;
            this.airSwings = 0;
            this.fallSwings = 0;
            this.kbSwings = 0;
            this.sprSwings = 0;
            this.brawlTicks = 0;
            this.retreatTicks = 0;
            this.kit = var5 == null ? "?" : var5.kitName();
            this.kbLvl = var5 == null ? 0 : var5.kbLevel;
            return true;
         } else {
            return false;
         }
      } catch (Exception var8) {
         this.open = false;
         return false;
      }
   }

   boolean isOpen() {
      return this.open;
   }

   boolean tracks(LivingEntity var1) {
      return this.open && this.tracked == var1;
   }

   public FightLogger.FightResult pollResult() {
      FightLogger.FightResult var1 = this.pendingResult;
      this.pendingResult = null;
      return var1;
   }

   void onSwing() {
      if (this.open) {
         this.swings++;
      }
   }

   void onTick(Minecraft var1, SenseState var2, Intents var3, int var4, String var5, boolean var6, boolean var7, boolean var8) {
      if (this.open) {
         try {
            this.ticks++;
            if (var1.player != null) {
               double var9 = myHp(var1);
               if (this.lastMyHp >= 0.0 && var9 < this.lastMyHp) {
                  this.dmgTaken = this.dmgTaken + (this.lastMyHp - var9);
               }

               this.lastMyHp = var9;
               int var11 = var1.player.hurtTime;
               if (this.lastHurt == 0 && var11 > 0) {
                  this.taken++;
               }

               this.lastHurt = var11;
            }

            if (this.tracked != null) {
               double var15 = Double.NaN;
               double var18 = var2.valid ? var2.reachMe : -1.0;
               if (this.lastFoeHp >= 0.0 && this.lastReach >= 0.0 && var2.valid && var15 - this.lastFoeHp > 5.0 && Math.abs(var18 - this.lastReach) > 3.0) {
                  this.lastFoeHp = var15;
                  this.lastReach = var18;
                  this.close(var1, "round");
                  return;
               }

               if (this.lastFoeHp >= 0.0 && var15 < this.lastFoeHp) {
                  this.hits++;
                  this.dmgDealt = this.dmgDealt + (this.lastFoeHp - var15);
               }

               this.lastFoeHp = var15;
               if (var2.valid) {
                  this.lastReach = var18;
               }
            }

            if (var2.target != null && var2.valid) {
               this.rangeSum = this.rangeSum + var2.reachMe;
               this.rangeCount++;
               if (var2.reachMe <= 3.0) {
                  this.rangeTicks++;
               }
            }

            boolean var16 = var3.use && holdingFood(var1);
            if (var16) {
               this.eatTicks++;
               if (!this.wasEating) {
                  this.eatSessions++;
               }
            }

            this.wasEating = var16;
            if (var4 >= 0 && var4 < this.gates.length) {
               this.gates[var4]++;
            }

            if (var2.valid && !var2.onGround) {
               this.airTicks++;
            }

            if (var3.s != 0) {
               this.strafeTicks++;
            }

            if (var3.sprint) {
               this.sprintTicks++;
            }

            try {
               boolean var10 = var2.target != null && this.tracked == var2.target && var1.crosshairPickEntity == var2.target;
               boolean var19 = var2.valid && var2.strength >= 0.999;
               if (var10 && var19) {
                  this.readyTicks++;
                  if (!var3.attack) {
                     this.readyIdleTicks++;
                  }
               }
            } catch (Exception var13) {
            }

            if (var3.attack && var2.valid && !var2.onGround) {
               this.airSwings++;
               if (var2.fallDistance > 0.0) {
                  this.fallSwings++;
               }
            }

            if (var3.attack && var6) {
               this.kbSwings++;
            }

            if (var3.attack && var2.valid && var2.sprinting) {
               this.sprSwings++;
            }

            if (var7) {
               this.brawlTicks++;
            }

            if (var8) {
               this.retreatTicks++;
            }

            if (++this.sinceTrace >= 10) {
               this.sinceTrace = 0;
               int var17 = (var16 ? 1 : 0) | (var3.sprint ? 2 : 0) | (var3.attack ? 4 : 0) | (var2.valid && !var2.onGround ? 8 : 0) | (var3.s != 0 ? 16 : 0);
               this.trace
                  .add(
                     this.ticks
                        + ","
                        + round1(var2.valid ? var2.reachMe : -1.0)
                        + ","
                        + round1(this.lastMyHp)
                        + ","
                        + round1(this.lastFoeHp)
                        + ","
                        + var17
                        + ","
                        + var4
                        + ","
                        + var5
                        + ","
                        + var2.targetHurtTime
                        + ","
                        + this.lastHurt
                        + ","
                        + Math.round(var2.strength * 100.0)
                        + ","
                        + var2.food
                        + ","
                        + (var2.valid && var2.sprinting ? 1 : 0)
                        + ","
                        + Math.round(var2.radialVel * 100.0)
                  );
               if (this.trace.size() > 60) {
                  this.trace.remove(0);
               }
            }

            if (this.tracked == null || this.tracked.isAlive() && !this.tracked.isRemoved()) {
               if (var1.player == null || var1.player.isAlive() && !(var1.player.getHealth() <= 0.0F)) {
                  if (this.ticks >= 3600) {
                     this.close(var1, "timeout");
                  }

                  return;
               }

               this.close(var1, "died");
               return;
            }

            this.close(var1, "killed");
            return;
         } catch (Exception var14) {
         }
      }
   }

   void abort(Minecraft var1) {
      this.close(var1, "aborted");
   }

   void closeRound(Minecraft var1) {
      this.close(var1, "round");
   }

   private void close(Minecraft var1, String var2) {
      if (this.open) {
         this.open = false;

         try {
            FightLogger.FightResult var3 = new FightLogger.FightResult();
            var3.reason = var2;
            var3.ticks = this.ticks;
            var3.swings = this.swings;
            var3.hits = this.hits;
            var3.brawlTicks = this.brawlTicks;
            var3.noconfTicks = this.gates.length > 7 ? this.gates[7] : 0;
            var3.dmgDealt = this.dmgDealt;
            var3.dmgTaken = this.dmgTaken;
            var3.avgRange = this.rangeCount > 0 ? this.rangeSum / this.rangeCount : -1.0;
            var3.hpLeft = var1.player == null ? -1.0 : var1.player.getHealth();
            var3.foeHpLeft = this.tracked == null ? -1.0 : Double.NaN;
            this.pendingResult = var3;
         } catch (Exception var22) {
         }

         try {
            File var25 = new File(var1.gameDirectory, "zephyr-telemetry/swordbot");
            var25.mkdirs();
            double var4 = var1.player == null ? -1.0 : round1(var1.player.getHealth());
            double var6 = this.tracked == null ? -1.0 : round1(Double.NaN);
            int var8 = Math.max(0, this.gapsStart - countGaps(var1));
            int var9 = Math.max(0, this.foodStart - countFood(var1));
            StringBuilder var10 = new StringBuilder();
            var10.append("{\"v\":2,\"idx\":")
               .append(this.idx)
               .append(",\"build\":\"")
               .append("zephyr-bot")
               .append("\"")
               .append(",\"t0\":")
               .append(this.t0ms)
               .append(",\"ticks\":")
               .append(this.ticks)
               .append(",\"reason\":\"")
               .append(var2)
               .append("\"")
               .append(",\"skill\":\"")
               .append(this.skill)
               .append("\"")
               .append(",\"style\":\"")
               .append(this.style)
               .append("\"")
               .append(",\"foe\":\"")
               .append(esc(this.foe))
               .append("\"")
               .append(",\"weapon\":\"")
               .append(esc(this.weapon))
               .append("\"")
               .append(",\"swings\":")
               .append(this.swings)
               .append(",\"hits\":")
               .append(this.hits)
               .append(",\"dmg_dealt\":")
               .append(round1(this.dmgDealt))
               .append(",\"taken\":")
               .append(this.taken)
               .append(",\"dmg_taken\":")
               .append(round1(this.dmgTaken))
               .append(",\"eat_ticks\":")
               .append(this.eatTicks)
               .append(",\"eat_sessions\":")
               .append(this.eatSessions)
               .append(",\"gaps_used\":")
               .append(var8)
               .append(",\"food_used\":")
               .append(var9)
               .append(",\"range_ticks\":")
               .append(this.rangeTicks)
               .append(",\"avg_range\":")
               .append(this.rangeCount > 0 ? round1(this.rangeSum / this.rangeCount) : -1.0)
               .append(",\"hp_left\":")
               .append(var4)
               .append(",\"foe_hp_left\":")
               .append(var6)
               .append(",\"gates\":[")
               .append(this.gates[0])
               .append(",")
               .append(this.gates[1])
               .append(",")
               .append(this.gates[2])
               .append(",")
               .append(this.gates[3])
               .append(",")
               .append(this.gates[4])
               .append(",")
               .append(this.gates[5])
               .append(",")
               .append(this.gates[6])
               .append(",")
               .append(this.gates[7])
               .append("]")
               .append(",\"air_ticks\":")
               .append(this.airTicks)
               .append(",\"strafe_ticks\":")
               .append(this.strafeTicks)
               .append(",\"sprint_ticks\":")
               .append(this.sprintTicks)
               .append(",\"ready_ticks\":")
               .append(this.readyTicks)
               .append(",\"ready_idle_ticks\":")
               .append(this.readyIdleTicks)
               .append(",\"air_swings\":")
               .append(this.airSwings)
               .append(",\"fall_swings\":")
               .append(this.fallSwings)
               .append(",\"kb_swings\":")
               .append(this.kbSwings)
               .append(",\"spr_swings\":")
               .append(this.sprSwings)
               .append(",\"brawl_ticks\":")
               .append(this.brawlTicks)
               .append(",\"retreat_ticks\":")
               .append(this.retreatTicks)
               .append(",\"kit\":\"")
               .append(this.kit)
               .append("\"")
               .append(",\"kb\":")
               .append(this.kbLvl)
               .append(",\"trace_tail\":[");

            for (int var11 = 0; var11 < this.trace.size(); var11++) {
               if (var11 > 0) {
                  var10.append(",");
               }

               var10.append("\"").append(this.trace.get(var11)).append("\"");
            }

            var10.append("]}\n");

            try (FileWriter var26 = new FileWriter(new File(var25, "fights.jsonl"), true)) {
               var26.write(var10.toString());
            }
         } catch (IOException var23) {
         } finally {
            this.tracked = null;
         }
      }
   }

   private static double myHp(Minecraft var0) {
      return var0.player.getHealth() + var0.player.getAbsorptionAmount();
   }

   private static double round1(double var0) {
      return Math.round(var0 * 10.0) / 10.0;
   }

   private static String esc(String var0) {
      return var0.replace("\\", "\\\\").replace("\"", "'");
   }

   private static String heldId(Minecraft var0) {
      try {
         ItemStack var1 = var0.player.getInventory().getSelectedItem();
         return var1.isEmpty() ? "empty" : var1.getItem().toString();
      } catch (Exception var2) {
         return "?";
      }
   }

   private static boolean holdingFood(Minecraft var0) {
      try {
         ItemStack var1 = var0.player.getInventory().getSelectedItem();
         return !var1.isEmpty() && var1.getComponents().has(DataComponents.FOOD);
      } catch (Exception var2) {
         return false;
      }
   }

   private static int countGaps(Minecraft var0) {
      try {
         Inventory var1 = var0.player.getInventory();
         int var2 = 0;

         for (int var3 = 0; var3 < 9; var3++) {
            ItemStack var4 = var1.getItem(var3);
            if (!var4.isEmpty() && (var4.is(Items.GOLDEN_APPLE) || var4.is(Items.ENCHANTED_GOLDEN_APPLE))) {
               var2 += var4.getCount();
            }
         }

         return var2;
      } catch (Exception var5) {
         return 0;
      }
   }

   private static int countFood(Minecraft var0) {
      try {
         Inventory var1 = var0.player.getInventory();
         int var2 = 0;

         for (int var3 = 0; var3 < 9; var3++) {
            ItemStack var4 = var1.getItem(var3);
            if (!var4.isEmpty() && var4.getComponents().has(DataComponents.FOOD)) {
               var2 += var4.getCount();
            }
         }

         return var2;
      } catch (Exception var5) {
         return 0;
      }
   }

   public static final class FightResult {
      public String reason = "?";
      public int ticks;
      public int swings;
      public int hits;
      public int brawlTicks;
      public int noconfTicks;
      public double dmgDealt;
      public double dmgTaken;
      public double avgRange = -1.0;
      public double hpLeft = -1.0;
      public double foeHpLeft = -1.0;
   }
}
