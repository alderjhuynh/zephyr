package com.zephyr.client.module.bot.sword;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public final class KitProfile {
   public KitProfile.Archetype type = KitProfile.Archetype.PLAIN;
   public int plainSlot = -1;
   public int kbSlot = -1;
   public int kbLevel;
   public boolean wieldKb;
   private boolean scanned;

   public void rescan(Minecraft var1) {
      this.plainSlot = -1;
      this.kbSlot = -1;
      this.kbLevel = 0;
      this.wieldKb = false;
      this.scanned = true;
      if (var1 != null && var1.player != null) {
         Inventory var2 = var1.player.getInventory();

         for (int var3 = 0; var3 < 9; var3++) {
            ItemStack var4 = var2.getItem(var3);
            if (!var4.isEmpty() && kbLevelOf(var4, var1) <= 0 && var4.is(ItemTags.SWORDS)) {
               this.plainSlot = var3;
               break;
            }
         }

         if (this.plainSlot < 0) {
            for (int var6 = 0; var6 < 9; var6++) {
               ItemStack var8 = var2.getItem(var6);
               if (!var8.isEmpty() && kbLevelOf(var8, var1) <= 0 && (var8.is(ItemTags.AXES) || RecoverBrain.isWeapon(var8))) {
                  this.plainSlot = var6;
                  break;
               }
            }
         }

         for (int var7 = 0; var7 < 9; var7++) {
            ItemStack var9 = var2.getItem(var7);
            if (!var9.isEmpty()) {
               int var5 = kbLevelOf(var9, var1);
               if (var5 > 0 && (var9.is(ItemTags.SWORDS) || var9.is(ItemTags.AXES) || RecoverBrain.isWeapon(var9))) {
                  this.kbSlot = var7;
                  this.kbLevel = var5;
                  break;
               }
            }
         }

         if (this.kbSlot < 0) {
            this.type = KitProfile.Archetype.PLAIN;
         } else if (this.plainSlot < 0) {
            this.type = KitProfile.Archetype.KB_ONLY;
         } else {
            this.type = KitProfile.Archetype.KB_DUAL;
         }

         this.refreshWield(var1);
      } else {
         this.type = KitProfile.Archetype.PLAIN;
      }
   }

   public void refreshWield(Minecraft var1) {
      if (var1 != null && var1.player != null && this.scanned) {
         int var2 = var1.player.getInventory().getSelectedSlot();
         this.wieldKb = this.kbSlot >= 0 && var2 == this.kbSlot;
      } else {
         this.wieldKb = false;
      }
   }

   public boolean needsScan(int var1) {
      return !this.scanned || var1 % 40 == 0;
   }

   public boolean hasKb() {
      return this.kbSlot >= 0;
   }

   public boolean kbMode() {
      return this.hasKb() && this.wieldKb;
   }

   public String kitName() {
      return switch (this.type) {
         case KB_DUAL -> "KB_DUAL";
         case KB_ONLY -> "KB_ONLY";
         default -> "PLAIN";
      };
   }

   public int doctrineSlot(Minecraft var1, SenseState var2, BotConfig var3) {
      if (!this.hasKb()) {
         return this.plainSlot;
      } else {
         int var4 = this.plainSlot >= 0 ? this.plainSlot : this.kbSlot;
         if (var1 != null && var1.player != null && var2 != null) {
            double var5 = var2.health + var2.absorption;
            boolean var7 = RecoverBrain.findGapSlot(var1) >= 0;
            boolean var8 = var5 <= var3.gapHpThreshold && var7;
            boolean var9 = var5 <= var3.critHpThreshold && var7;
            boolean var10 = var2.target != null && var2.targetVisible && var2.reachOp <= 4.5;
            return (!var8 || !var10) && !var9 ? var4 : this.kbSlot;
         } else {
            return var4;
         }
      }
   }

   public boolean ensureWeaponForAttack(Minecraft var1, SenseState var2, BotConfig var3) {
      if (var1 != null && var1.player != null) {
         int var4 = this.doctrineSlot(var1, var2, var3);
         Inventory var5 = var1.player.getInventory();
         if (var4 >= 0 && var5.getSelectedSlot() != var4) {
            var5.setSelectedSlot(var4);
            this.refreshWield(var1);
            return false;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public void applyDoctrine(Minecraft var1, SenseState var2, BotConfig var3, RecoverBrain var4, BowBrain var5, Intents var6) {
      if (var1 != null && var1.player != null && var2 != null && !var6.use && var4.mode() != 2 && !var5.drawing()) {
         int var7 = this.doctrineSlot(var1, var2, var3);
         Inventory var8 = var1.player.getInventory();
         if (var7 >= 0 && var8.getSelectedSlot() != var7) {
            var8.setSelectedSlot(var7);
            this.refreshWield(var1);
         }
      }
   }

   private static int kbLevelOf(ItemStack var0, Minecraft var1) {
      try {
         if (var1 != null && var1.level != null && !var0.isEmpty()) {
            Registry var2 = var1.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Reference var3 = var2.getOrThrow(Enchantments.KNOCKBACK);
            return EnchantmentHelper.getItemEnchantmentLevel(var3, var0);
         } else {
            return 0;
         }
      } catch (Throwable var4) {
         return 0;
      }
   }

   public enum Archetype {
      PLAIN,
      KB_DUAL,
      KB_ONLY;
   }
}
