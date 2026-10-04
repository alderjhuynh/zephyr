package com.zephyr.client.module.bot.sword;

import com.zephyr.client.mixin.bot.SwordBotMinecraftInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

final class Motor {
   boolean apply(Minecraft var1, Intents var2, Aim var3, BotConfig var4, boolean var5, LivingEntity var6) {
      boolean var7 = false;
      if (var1.player != null && var1.options != null) {
         if (var5) {
            float[] var8 = var3.step(var4);
            var1.player.setYRot(Mth.wrapDegrees(var8[0]));
            var1.player.setXRot(Mth.clamp(Mth.wrapDegrees(var8[1]), -90.0F, 90.0F));
         }

         var1.options.keyUp.setDown(var2.f > 0);
         var1.options.keyDown.setDown(var2.f < 0);
         var1.options.keyLeft.setDown(var2.s > 0);
         var1.options.keyRight.setDown(var2.s < 0);
         var1.options.keyJump.setDown(var2.jump);
         var1.options.keySprint.setDown(var2.sprint && var2.f > 0);
         if (var2.attack && !var1.player.isUsingItem()) {
            SwordBotMinecraftInvoker var10 = (SwordBotMinecraftInvoker)var1;
            var10.swordBot$pick(1.0F);
            if (var6 != null && var1.crosshairPickEntity == var6) {
               float var9 = var1.player.getAttackStrengthScale(0.5F);
               var10.swordBot$startAttack();
               var7 = var1.player.getAttackStrengthScale(0.5F) < var9;
            }

            var1.options.keyAttack.setDown(false);
         } else {
            var1.options.keyAttack.setDown(false);
         }

         var1.options.keyUse.setDown(var2.use);
      }

      return var7;
   }

   void releaseAll(Minecraft var1) {
      if (var1.options != null) {
         var1.options.keyUp.setDown(false);
         var1.options.keyDown.setDown(false);
         var1.options.keyLeft.setDown(false);
         var1.options.keyRight.setDown(false);
         var1.options.keyJump.setDown(false);
         var1.options.keySprint.setDown(false);
         var1.options.keyAttack.setDown(false);
         var1.options.keyUse.setDown(false);
      }
   }
}
