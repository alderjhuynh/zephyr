package com.zephyr.client.module.bot.sword;

public final class Intents {
   public int f;
   public int s;
   public boolean jump;
   public boolean sprint;
   public boolean attack;
   public boolean use;

   public void clear() {
      this.f = 0;
      this.s = 0;
      this.jump = false;
      this.sprint = false;
      this.attack = false;
      this.use = false;
   }

   public void set(int var1, int var2, boolean var3, boolean var4) {
      this.f = var1;
      this.s = var2;
      this.jump = var3;
      this.sprint = var4;
   }
}
