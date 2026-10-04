package com.zephyr.client.module.bot.sword;

public final class BotConfig {
   public BotConfig.Skill skill = BotConfig.Skill.OPTIMAL;
   public BotConfig.Style style = BotConfig.Style.BALANCED;
   public double gapHpThreshold = 12.0;
   public double critHpThreshold = 7.0;
   public boolean allowBow = true;
   public boolean allowEat = true;
   public double bowMinDist = 13.0;
   public double bowMaxDist = 30.0;
   public double punishEaterDist = 6.0;
   public boolean autoTarget = true;
   public double autoTargetRange = 100.0;
   public boolean cloneDump = false;
   public boolean nnEnabled = false;
   public double nnStrafeScale = 1.0;
   public double nnCommitReach = 2.7;
   public int nnTapLen = 2;

   private BotConfig() {
   }

   public static BotConfig defaults() {
      return new BotConfig();
   }

   public enum Skill {
      POTATO(15, 12.0, 6, 6.0, 120, 10),
      AVERAGE(50, 30.0, 3, 2.5, 50, 5),
      PRO(80, 60.0, 2, 0.8, 15, 2),
      ELITE(92, 120.0, 1, 0.3, 5, 1),
      OPTIMAL(100, 180.0, 1, 0.0, 0, 0);

      public final int skill;
      public final double turnDegPerTick;
      public final int thinkTicks;
      public final double aimErrDeg;
      public final int mistakePerMille;
      public final int reactTicks;

      Skill(int nullxx, double nullxxx, int nullxxxx, double nullxxxxx, int nullxxxxxx, int nullxxxxxxx) {
         this.skill = nullxx;
         this.turnDegPerTick = nullxxx;
         this.thinkTicks = nullxxxx;
         this.aimErrDeg = nullxxxxx;
         this.mistakePerMille = nullxxxxxx;
         this.reactTicks = nullxxxxxxx;
      }
   }

   public enum Style {
      AGGRESSIVE(85, 25),
      BALANCED(50, 50),
      DEFENSIVE(30, 80);

      public final int aggression;
      public final int caution;

      Style(int nullxx, int nullxxx) {
         this.aggression = nullxx;
         this.caution = nullxxx;
      }
   }
}
