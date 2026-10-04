package com.zephyr.client.module.bot.sword;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class RecoverBrain {
   private int mode;
   private int modeAge;
   private int mealCount;
   private boolean hasStartedMeal;
   private int giveUpUntil;

   boolean tick(Minecraft var1, SenseState var2, BotConfig var3, Intents var4, int var5) {
      var4.clear();
      if (var3.allowEat && var1.player != null) {
         double var6 = var2.health + var2.absorption;
         int var8 = this.wantLevel(var1, var2, var3, var6);
         if (var8 > 0 && var2.target != null && var2.targetVisible && var2.targetHealth <= 8.0 && var2.hdist < 6.0) {
            var8 = 0;
         }

         double var9 = window(var2, var5);
         if (this.mode == 3 && var5 >= this.giveUpUntil) {
            this.mode = 0;
         }

         if (this.mode == 0) {
            if (var8 == 0) {
               return false;
            } else {
               return mealWindow(var8, var9) && this.startMeal(var1, var2, var8, var5) ? eatTick(var2, var4, var5) : false;
            }
         } else if (this.mode == 2) {
            int var11 = var1.player.getInventory().getSelectedSlot();
            if (isEdibleNow(var1, var11, var2.food) && var1.player.getInventory().getItem(var11).getCount() == this.mealCount && var5 - this.modeAge < 45) {
               return eatTick(var2, var4, var5);
            }

            this.mode = 0;
            selectWeapon(var1);
            return false;
         } else {
            return false;
         }
      } else {
         this.mode = 0;
         return false;
      }
   }

   private static boolean eatTick(SenseState var0, Intents var1, int var2) {
      var1.f = -1;
      var1.use = true;
      return true;
   }

   private static boolean mealWindow(int var0, double var1) {
      return var0 == 1 && var1 >= 36.0 || var0 == 2 && var1 >= 8.0 || var0 >= 3 && var1 >= 20.0;
   }

   private boolean startMeal(Minecraft var1, SenseState var2, int var3, int var4) {
      int var5;
      if (var3 >= 2) {
         var5 = findGapSlot(var1);
         if (var5 < 0) {
            return false;
         }
      } else {
         var5 = findRegularFoodSlot(var1);
         if (var5 < 0) {
            var5 = findGapSlot(var1);
         }

         if (var5 < 0 || !isEdibleNow(var1, var5, var2.food)) {
            return false;
         }
      }

      this.mode = 2;
      this.hasStartedMeal = true;
      this.modeAge = var4;
      selectSlot(var1, var5);
      this.mealCount = var1.player.getInventory().getItem(var5).getCount();
      return true;
   }

   private int wantLevel(Minecraft var1, SenseState var2, BotConfig var3, double var4) {
      boolean var6 = findFoodSlot(var1, false) >= 0;
      boolean var7 = findGapSlot(var1) >= 0;
      if (!this.hasStartedMeal && var7 && var2.absorption == 0.0 && var2.health >= 18.0 && var2.targetVisible && var2.hdist >= 8.0) {
         return 2;
      } else if (var4 <= var3.critHpThreshold && var7) {
         return 3;
      } else if (var4 <= var3.gapHpThreshold && var7) {
         return 2;
      } else {
         return var2.food <= 17 && var6 ? 1 : 0;
      }
   }

   private static double window(SenseState var0, int var1) {
      if (var0.target != null && var0.targetVisible) {
         int var2 = MeleeBrain.opponentReadyIn(var0, var1);
         double var3 = 0.22 + Math.max(0.0, var0.radialVel);
         double var5 = Math.max(0.0, var0.reachOp - 3.0) / Math.max(0.05, var3);
         return Math.max(var2, var5);
      } else {
         return 200.0;
      }
   }

   static int findFoodSlot(Minecraft var0, boolean var1) {
      if (var0.player == null) {
         return -1;
      }

      Inventory var2 = var0.player.getInventory();

      for (int var3 = 0; var3 < 9; var3++) {
         ItemStack var4 = var2.getItem(var3);
         if (!var4.isEmpty()) {
            if (var4.is(Items.GOLDEN_APPLE) || var4.is(Items.ENCHANTED_GOLDEN_APPLE)) {
               return var3;
            }

            if (!var1 && var4.getComponents().has(DataComponents.FOOD)) {
               return var3;
            }
         }
      }

      return -1;
   }

   static int findGapSlot(Minecraft var0) {
      if (var0.player == null) {
         return -1;
      }

      Inventory var1 = var0.player.getInventory();

      for (int var2 = 0; var2 < 9; var2++) {
         ItemStack var3 = var1.getItem(var2);
         if (!var3.isEmpty() && (var3.is(Items.GOLDEN_APPLE) || var3.is(Items.ENCHANTED_GOLDEN_APPLE))) {
            return var2;
         }
      }

      return -1;
   }

   static int findRegularFoodSlot(Minecraft var0) {
      if (var0.player == null) {
         return -1;
      }

      Inventory var1 = var0.player.getInventory();

      for (int var2 = 0; var2 < 9; var2++) {
         ItemStack var3 = var1.getItem(var2);
         if (!var3.isEmpty() && !isGap(var3) && var3.getComponents().has(DataComponents.FOOD)) {
            return var2;
         }
      }

      return -1;
   }

   static boolean isEdibleNow(Minecraft var0, int var1, int var2) {
      if (var0.player != null && var1 >= 0 && var1 < 9) {
         ItemStack var3 = var0.player.getInventory().getItem(var1);
         if (var3.isEmpty()) {
            return false;
         } else {
            return isGap(var3) ? true : var3.getComponents().has(DataComponents.FOOD) && var2 < 20;
         }
      } else {
         return false;
      }
   }

   private static boolean isGap(ItemStack var0) {
      return var0.is(Items.GOLDEN_APPLE) || var0.is(Items.ENCHANTED_GOLDEN_APPLE);
   }

   static int findBowSlot(Minecraft var0) {
      if (var0.player == null) {
         return -1;
      }

      Inventory var1 = var0.player.getInventory();

      for (int var2 = 0; var2 < 9; var2++) {
         if (!var1.getItem(var2).isEmpty() && var1.getItem(var2).is(Items.BOW)) {
            return var2;
         }
      }

      return -1;
   }

   static boolean hasArrows(Minecraft var0) {
      if (var0.player == null) {
         return false;
      }

      Inventory var1 = var0.player.getInventory();

      for (int var2 = 0; var2 < var1.getContainerSize(); var2++) {
         ItemStack var3 = var1.getItem(var2);
         if (!var3.isEmpty() && (var3.is(Items.ARROW) || var3.is(Items.SPECTRAL_ARROW))) {
            return true;
         }
      }

      return false;
   }

   private static void selectSlot(Minecraft var0, int var1) {
      var0.player.getInventory().setSelectedSlot(var1);
   }

   int mode() {
      return this.mode;
   }

   static boolean isWeapon(ItemStack var0) {
      return !var0.isEmpty() && var0.getComponents().has(DataComponents.WEAPON);
   }

   static int findWeaponSlot(Minecraft var0) {
      if (var0.player == null) {
         return -1;
      }

      Inventory var1 = var0.player.getInventory();

      for (int var2 = 0; var2 < 9; var2++) {
         if (isWeapon(var1.getItem(var2))) {
            return var2;
         }
      }

      return -1;
   }

   static void selectWeapon(Minecraft var0) {
      if (var0.player != null) {
         Inventory var1 = var0.player.getInventory();
         if (!isWeapon(var1.getItem(var1.getSelectedSlot()))) {
            int var2 = findWeaponSlot(var0);
            if (var2 >= 0) {
               var1.setSelectedSlot(var2);
            }
         }
      }
   }

   void reset() {
      this.mode = 0;
      this.modeAge = 0;
      this.giveUpUntil = 0;
      this.hasStartedMeal = false;
   }
}
