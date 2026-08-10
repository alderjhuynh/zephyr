package com.zephyr.client.util;

import net.minecraft.util.FastColor;

public final class ARGB {
	private ARGB() {
	}

	public static int color(int r, int g, int b) {
		return FastColor.ARGB32.color(255, r, g, b);
	}

	public static int color(int r, int g, int b, int a) {
		return FastColor.ARGB32.color(a, r, g, b);
	}

	public static int opaque(int color) {
		return FastColor.ARGB32.opaque(color);
	}

	public static int multiplyAlpha(int color, float alpha) {
		return FastColor.ARGB32.colorFromFloat(
				FastColor.ARGB32.red(color) / 255.0F,
				FastColor.ARGB32.green(color) / 255.0F,
				FastColor.ARGB32.blue(color) / 255.0F,
				alpha);
	}

	public static int white(float alpha) {
		return FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, 1.0F, alpha);
	}
}
