package com.robotmon.rbm.util;

import com.robotmon.rbm.model.HsvColor;
import com.robotmon.rbm.model.RgbColor;

/** Ported from isSameColor()/absColor()/rgb2hsv() in index.js. */
public final class ColorUtils {
    private ColorUtils() {}

    public static boolean isSameColor(RgbColor c1, RgbColor c2, int diff) {
        return Math.abs(c1.r - c2.r) <= diff
            && Math.abs(c1.g - c2.g) <= diff
            && Math.abs(c1.b - c2.b) <= diff;
    }

    public static boolean isSameColor(RgbColor c1, RgbColor c2) {
        return isSameColor(c1, c2, 20);
    }

    /** Sum of absolute per-channel differences. Ported from absColor(). */
    public static int absColorDistance(RgbColor c1, RgbColor c2) {
        return Math.abs(c1.r - c2.r) + Math.abs(c1.g - c2.g) + Math.abs(c1.b - c2.b);
    }

    /** input: rgb in [0,255]; output: h in [0,360), s/v in [0,100]. Ported from rgb2hsv(). */
    public static HsvColor rgb2hsv(RgbColor rgb) {
        double r = rgb.r / 255.0;
        double g = rgb.g / 255.0;
        double b = rgb.b / 255.0;
        double v = Math.max(r, Math.max(g, b));
        double c = v - Math.min(r, Math.min(g, b));
        double h;
        if (c == 0) {
            h = 0;
        } else if (v == r) {
            h = (g - b) / c;
        } else if (v == g) {
            h = 2 + (b - r) / c;
        } else {
            h = 4 + (r - g) / c;
        }
        h = 60 * (h < 0 ? h + 6 : h);
        double s = v == 0 ? 0 : Math.round(c / v * 100);
        return new HsvColor(h, s, Math.round(v * 100));
    }
}