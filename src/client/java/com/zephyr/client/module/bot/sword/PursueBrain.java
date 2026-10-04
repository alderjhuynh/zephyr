package com.zephyr.client.module.bot.sword;

final class PursueBrain {
   boolean tick(SenseState var1, Targeting var2, Intents var3) {
      var3.clear();
      if (var1.target == null) {
         return false;
      }

      if (var1.targetVisible) {
         boolean var4 = var1.hdist >= 7.0;
         boolean var5 = var1.hdist >= 5.0 && var1.radialVel < -0.1;
         if (!var4 && !var5) {
            return false;
         }

         var3.f = 1;
         var3.sprint = true;
         double var6 = 0.35 + Math.max(0.0, var1.radialVel);
         boolean var8 = var1.foeKbLevel > 0 && var1.hdist <= 6.0;
         if (!var8 && var1.onGround && var1.sprinting && var1.hdist >= 6.5 && var1.hdist <= 8.0 && var1.radialVel >= 0.1) {
            var3.jump = true;
         }

         return true;
      } else {
         if (!var2.hasLastSeen()) {
            return false;
         }

         var3.f = 1;
         var3.sprint = true;
         if (var1.hdist >= 4.0 && var1.onGround && (var1.foeKbLevel <= 0 || !(var1.hdist <= 6.0))) {
            var3.jump = true;
         }

         return true;
      }
   }
}
