package com.zephyr.client.module.bot.sword;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Random;

public final class NnBrain {
   private static final int IN = 8;
   private static final int HIDDEN = 10;
   private static final int NSTYLE = 3;
   private static final int NCONT = 3;
   private static final double LR = 0.02;
   private static final double SIGMA = 0.25;
   private static final double BASELINE_LR = 0.1;
   private static final int SHUTDOWN_STREAK = 10;
   private final Random random = new Random();
   private double[][] w1 = new double[10][8];
   private double[] b1 = new double[10];
   private double[][] w2s = new double[3][10];
   private double[] b2s = new double[3];
   private double[][] w2c = new double[3][10];
   private double[] b2c = new double[]{0.0, 0.203, 0.0};
   private double baseline;
   private int fights;
   private int wins;
   private int consecWins;
   private int holdRounds;
   private boolean loaded;
   private File file;
   private double[] lastX;
   private double[] lastH;
   private int lastStyle = 1;
   private double[] lastProbs = new double[]{0.3333333333333333, 0.3333333333333333, 0.3333333333333333};
   private double[] lastMeans = new double[3];
   private double[] lastCont = new double[3];
   private boolean hasPending;
   private double lastWin;
   private double lastMargin;
   private double lastAcc;
   private double lastRange;
   private double lastBrawl;
   private double lastNoconf;
   private double lastHp;
   private double lastFoeHp;

   public NnBrain() {
      for (int var1 = 0; var1 < 10; var1++) {
         for (int var2 = 0; var2 < 8; var2++) {
            this.w1[var1][var2] = (this.random.nextDouble() * 2.0 - 1.0) * 0.1;
         }
      }

      for (int var3 = 0; var3 < 3; var3++) {
         for (int var5 = 0; var5 < 10; var5++) {
            this.w2s[var3][var5] = (this.random.nextDouble() * 2.0 - 1.0) * 0.1;
         }
      }

      for (int var4 = 0; var4 < 3; var4++) {
         for (int var6 = 0; var6 < 10; var6++) {
            this.w2c[var4][var6] = (this.random.nextDouble() * 2.0 - 1.0) * 0.1;
         }
      }
   }

   public void noteManualStyle() {
      this.holdRounds = 5;
   }

   public void onFightBegin(File var1, BotConfig var2) {
      this.ensureLoaded(var1);
      if (this.holdRounds > 0) {
         this.holdRounds--;
      } else if (var2.nnEnabled) {
         double[] var3 = this.features();
         double[] var4 = this.forwardHidden(var3);
         double[] var5 = new double[3];

         for (int var6 = 0; var6 < 3; var6++) {
            var5[var6] = this.b2s[var6];

            for (int var7 = 0; var7 < 10; var7++) {
               var5[var6] += this.w2s[var6][var7] * var4[var7];
            }
         }

         double[] var14 = softmax(var5);
         int var15 = this.sampleCategorical(var14);
         double[] var8 = new double[3];
         double[] var9 = new double[3];

         for (int var10 = 0; var10 < 3; var10++) {
            double var11 = this.b2c[var10];

            for (int var13 = 0; var13 < 10; var13++) {
               var11 += this.w2c[var10][var13] * var4[var13];
            }

            var11 = Math.tanh(var11);
            var8[var10] = var11;
            var9[var10] = clamp(var11 + this.random.nextGaussian() * 0.25, -1.0, 1.0);
         }

         this.lastX = var3;
         this.lastH = var4;
         this.lastStyle = var15;
         this.lastProbs = var14;
         this.lastMeans = var8;
         this.lastCont = var9;
         this.hasPending = true;
         var2.style = BotConfig.Style.values()[var15];
         applyKnobs(var2, var9);
      }
   }

   public boolean onFightEnd(File var1, BotConfig var2, FightLogger.FightResult var3) {
      this.ensureLoaded(var1);
      this.fights++;
      boolean var4 = "killed".equals(var3.reason) || "died".equals(var3.reason) || "timeout".equals(var3.reason);
      double var5;
      if ("killed".equals(var3.reason)) {
         this.wins++;
         this.consecWins++;
         var5 = 1.0;
      } else if ("died".equals(var3.reason)) {
         this.consecWins = 0;
         var5 = -1.0;
      } else {
         if (var4) {
            this.consecWins = 0;
         }

         var5 = 0.0;
      }

      double var7 = (var3.dmgDealt - var3.dmgTaken) / 20.0;
      var5 += 0.15 * Math.tanh(var7);
      this.lastWin = "killed".equals(var3.reason) ? 1.0 : ("died".equals(var3.reason) ? -1.0 : 0.0);
      this.lastMargin = clamp(var7, -1.0, 1.0);
      this.lastAcc = var3.swings > 0 ? (double)var3.hits / var3.swings : 0.0;
      this.lastRange = var3.avgRange >= 0.0 ? clamp(var3.avgRange / 5.0, 0.0, 2.0) : 1.0;
      this.lastBrawl = var3.ticks > 0 ? clamp((double)var3.brawlTicks / var3.ticks, 0.0, 1.0) : 0.0;
      this.lastNoconf = var3.ticks > 0 ? clamp((double)var3.noconfTicks / var3.ticks, 0.0, 1.0) : 0.0;
      this.lastHp = clamp(var3.hpLeft / 20.0, 0.0, 1.0);
      this.lastFoeHp = clamp(var3.foeHpLeft / 20.0, 0.0, 1.0);
      if (this.hasPending && var4 && var2.nnEnabled && this.holdRounds <= 0) {
         double var9 = var5 - this.baseline;
         double[] var11 = new double[3];

         for (int var12 = 0; var12 < 3; var12++) {
            var11[var12] = ((var12 == this.lastStyle ? 1.0 : 0.0) - this.lastProbs[var12]) * var9;
         }

         double[] var19 = new double[3];

         for (int var13 = 0; var13 < 3; var13++) {
            double var14 = this.lastMeans[var13];
            var19[var13] = (this.lastCont[var13] - var14) * (1.0 - var14 * var14) / 0.0625 * var9;
         }

         double[] var20 = new double[10];

         for (int var21 = 0; var21 < 10; var21++) {
            double var15 = 0.0;

            for (int var17 = 0; var17 < 3; var17++) {
               var15 += this.w2s[var17][var21] * var11[var17];
            }

            for (int var28 = 0; var28 < 3; var28++) {
               var15 += this.w2c[var28][var21] * var19[var28];
            }

            var20[var21] = var15 * (1.0 - this.lastH[var21] * this.lastH[var21]);
         }

         for (int var22 = 0; var22 < 3; var22++) {
            this.b2s[var22] = this.b2s[var22] + 0.02 * var11[var22];

            for (int var25 = 0; var25 < 10; var25++) {
               this.w2s[var22][var25] = this.w2s[var22][var25] + 0.02 * var11[var22] * this.lastH[var25];
            }
         }

         for (int var23 = 0; var23 < 3; var23++) {
            this.b2c[var23] = this.b2c[var23] + 0.02 * var19[var23];

            for (int var26 = 0; var26 < 10; var26++) {
               this.w2c[var23][var26] = this.w2c[var23][var26] + 0.02 * var19[var23] * this.lastH[var26];
            }
         }

         for (int var24 = 0; var24 < 10; var24++) {
            this.b1[var24] = this.b1[var24] + 0.02 * var20[var24];

            for (int var27 = 0; var27 < 8; var27++) {
               this.w1[var24][var27] = this.w1[var24][var27] + 0.02 * var20[var24] * this.lastX[var27];
            }
         }

         this.baseline = this.baseline + 0.1 * (var5 - this.baseline);
      }

      this.hasPending = false;
      this.save();
      return this.consecWins >= 10;
   }

   public String statusLine() {
      return String.format("nn fights=%d wins=%d consec=%d base=%.2f", this.fights, this.wins, this.consecWins, this.baseline);
   }

   private double[] features() {
      return new double[]{this.lastWin, this.lastMargin, this.lastAcc, this.lastRange, this.lastBrawl, this.lastNoconf, this.lastHp, this.lastFoeHp};
   }

   private double[] forwardHidden(double[] var1) {
      double[] var2 = new double[10];

      for (int var3 = 0; var3 < 10; var3++) {
         double var4 = this.b1[var3];

         for (int var6 = 0; var6 < 8; var6++) {
            var4 += this.w1[var3][var6] * var1[var6];
         }

         var2[var3] = Math.tanh(var4);
      }

      return var2;
   }

   private static void applyKnobs(BotConfig var0, double[] var1) {
      var0.nnStrafeScale = 0.6 + (var1[0] + 1.0) / 2.0 * 1.0;
      var0.nnCommitReach = 2.4 + (var1[1] + 1.0) / 2.0 * 0.5;
      var0.nnTapLen = 1 + (int)Math.round((var1[2] + 1.0) / 2.0 * 2.0);
      var0.nnTapLen = Math.max(1, Math.min(3, var0.nnTapLen));
   }

   private int sampleCategorical(double[] var1) {
      double var2 = this.random.nextDouble();
      double var4 = 0.0;

      for (int var6 = 0; var6 < var1.length; var6++) {
         var4 += var1[var6];
         if (var2 <= var4) {
            return var6;
         }
      }

      return var1.length - 1;
   }

   private static double[] softmax(double[] var0) {
      double var1 = var0[0];

      for (double var6 : var0) {
         var1 = Math.max(var1, var6);
      }

      double[] var8 = new double[var0.length];
      double var9 = 0.0;

      for (int var10 = 0; var10 < var0.length; var10++) {
         var8[var10] = Math.exp(var0[var10] - var1);
         var9 += var8[var10];
      }

      for (int var11 = 0; var11 < var8.length; var11++) {
         var8[var11] /= var9;
      }

      return var8;
   }

   private static double clamp(double var0, double var2, double var4) {
      return Math.max(var2, Math.min(var4, var0));
   }

   private void ensureLoaded(File var1) {
      if (!this.loaded) {
         this.loaded = true;
         if (var1 != null) {
            this.file = new File(var1, "zephyr-telemetry/swordbot/nn.json");

            try {
               if (!this.file.isFile()) {
                  return;
               }

               String var2 = Files.readString(this.file.toPath());
               ArrayList var3 = new ArrayList();
               StringBuilder var4 = new StringBuilder();

               for (int var5 = 0; var5 < var2.length(); var5++) {
                  char var6 = var2.charAt(var5);
                  if ((var6 < '0' || var6 > '9') && var6 != '-' && var6 != '+' && var6 != '.' && var6 != 'e' && var6 != 'E') {
                     if (var4.length() > 0) {
                        var3.add(Double.parseDouble(var4.toString()));
                        var4.setLength(0);
                     }
                  } else {
                     var4.append(var6);
                  }
               }

               if (var4.length() > 0) {
                  var3.add(Double.parseDouble(var4.toString()));
               }

               short var10 = 160;
               if (var3.size() < var10) {
                  return;
               }

               int var11 = 0;

               for (int var7 = 0; var7 < 10; var7++) {
                  for (int var8 = 0; var8 < 8; var8++) {
                     this.w1[var7][var8] = (Double)var3.get(var11++);
                  }
               }

               for (int var16 = 0; var16 < 10; var16++) {
                  this.b1[var16] = (Double)var3.get(var11++);
               }

               for (int var17 = 0; var17 < 3; var17++) {
                  for (int var21 = 0; var21 < 10; var21++) {
                     this.w2s[var17][var21] = (Double)var3.get(var11++);
                  }
               }

               for (int var18 = 0; var18 < 3; var18++) {
                  this.b2s[var18] = (Double)var3.get(var11++);
               }

               for (int var19 = 0; var19 < 3; var19++) {
                  for (int var22 = 0; var22 < 10; var22++) {
                     this.w2c[var19][var22] = (Double)var3.get(var11++);
                  }
               }

               for (int var20 = 0; var20 < 3; var20++) {
                  this.b2c[var20] = (Double)var3.get(var11++);
               }

               this.baseline = (Double)var3.get(var11++);
               this.fights = (int)Math.round((Double)var3.get(var11++));
               this.wins = (int)Math.round((Double)var3.get(var11++));
               this.consecWins = (int)Math.round((Double)var3.get(var11++));
            } catch (Exception var9) {
            }
         }
      }
   }

   private void save() {
      if (this.file != null) {
         try {
            StringBuilder var1 = new StringBuilder();
            var1.append("{\"w1\":[");
            appendMatrix(var1, this.w1);
            var1.append("],\"b1\":[");
            appendVector(var1, this.b1);
            var1.append("],\"w2s\":[");
            appendMatrix(var1, this.w2s);
            var1.append("],\"b2s\":[");
            appendVector(var1, this.b2s);
            var1.append("],\"w2c\":[");
            appendMatrix(var1, this.w2c);
            var1.append("],\"b2c\":[");
            appendVector(var1, this.b2c);
            var1.append("],\"baseline\":")
               .append(this.baseline)
               .append(",\"fights\":")
               .append(this.fights)
               .append(",\"wins\":")
               .append(this.wins)
               .append(",\"consec\":")
               .append(this.consecWins)
               .append("}\n");
            this.file.getParentFile().mkdirs();
            Files.writeString(this.file.toPath(), var1.toString());
         } catch (Exception var2) {
         }
      }
   }

   private static void appendMatrix(StringBuilder var0, double[][] var1) {
      boolean var2 = true;

      for (double[] var6 : var1) {
         for (double var10 : var6) {
            if (!var2) {
               var0.append(",");
            }

            var2 = false;
            var0.append(var10);
         }
      }
   }

   private static void appendVector(StringBuilder var0, double[] var1) {
      for (int var2 = 0; var2 < var1.length; var2++) {
         if (var2 > 0) {
            var0.append(",");
         }

         var0.append(var1[var2]);
      }
   }
}
