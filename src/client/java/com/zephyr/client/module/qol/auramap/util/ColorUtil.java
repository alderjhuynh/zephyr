package com.zephyr.client.module.qol.auramap.util;

public final class ColorUtil {
    private ColorUtil() {}

    public static int argb(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int rgb(int r, int g, int b) {
        return argb(0xFF, r, g, b);
    }

    public static int shadeRgb(int rgb, int shade) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int mul = switch (shade & 3) {
            case 0 -> 180;
            case 1 -> 220;
            case 3 -> 135;
            default -> 255;
        };
        r = r * mul / 255;
        g = g * mul / 255;
        b = b * mul / 255;
        return rgb(r, g, b);
    }

    public static int brightnessRgb(int rgb, float mult) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r = clamp((int) (r * mult));
        g = clamp((int) (g * mult));
        b = clamp((int) (b * mult));
        return rgb(r, g, b);
    }

    public static int desaturate(int rgb, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int gray = (r + g + b) / 3;
        r = (int) (r + (gray - r) * t);
        g = (int) (g + (gray - g) * t);
        b = (int) (b + (gray - b) * t);
        return rgb(r, g, b);
    }

    public static int applyHillShade(int rgb, int vSlope, int dSlope, int mode) {
        if (mode <= 0) return rgb;
        if (mode == 1) {

            if (vSlope > 0) return brightnessRgb(rgb, 1.10f);
            if (vSlope < 0) return brightnessRgb(rgb, 0.90f);
            return rgb;
        }

        float crossZ = -vSlope;
        float crossX = vSlope - dSlope;
        float cos;
        if (crossZ < 1.0f) {
            if (vSlope == 1 && dSlope == 1) {
                cos = 1.0f;
            } else {
                float cast = 1.0f - crossZ;
                float mag = (float) Math.sqrt(crossX * crossX + 1.0f + crossZ * crossZ);
                cos = (float) (cast / mag / Math.sqrt(2.0));
            }
        } else {
            cos = 0f;
        }
        float maxDirect = 0.6666667f;
        float direct;
        if (cos == 1.0f) direct = maxDirect;
        else if (cos > 0) direct = (float) Math.ceil(cos * 10f) / 10f * maxDirect * 0.88388f;
        else direct = 0f;
        float white = 0.5f + direct;
        float ambient = 0.2f;

        float mult = ambient + white;
        return brightnessRgb(rgb, mult);
    }

    public static int lerp(int aRgb, int bRgb, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (aRgb >> 16) & 0xFF, ag = (aRgb >> 8) & 0xFF, ab = aRgb & 0xFF;
        int br = (bRgb >> 16) & 0xFF, bg = (bRgb >> 8) & 0xFF, bb = bRgb & 0xFF;
        return rgb((int) (ar + (br - ar) * t), (int) (ag + (bg - ag) * t), (int) (ab + (bb - ab) * t));
    }

    public static int biomeTint(int baseRgb, int tintRgb) {
        int br = (baseRgb >> 16) & 0xFF, bg = (baseRgb >> 8) & 0xFF, bb = baseRgb & 0xFF;
        int tr = (tintRgb >> 16) & 0xFF, tg = (tintRgb >> 8) & 0xFF, tb = tintRgb & 0xFF;
        int rr = br * tr / 255;
        int gg = bg * tg / 255;
        int bb2 = bb * tb / 255;
        return rgb(rr, gg, bb2);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
