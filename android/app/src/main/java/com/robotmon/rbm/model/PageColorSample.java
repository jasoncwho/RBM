package com.robotmon.rbm.model;

/** One color-signature sample used to detect a game screen. Ported from a single entry in a Page.colors[] array. */
public class PageColorSample {
    /** Logical (1080-wide reference) screen coordinates. */
    public final int x;
    public final int y;
    public final RgbColor color;
    /** If true, the sampled pixel must be within `threshold` of `color`; if false, it must NOT be. */
    public final boolean match;
    public final int threshold;

    public PageColorSample(int x, int y, int r, int g, int b, boolean match, int threshold) {
        this.x = x;
        this.y = y;
        this.color = new RgbColor(r, g, b);
        this.match = match;
        this.threshold = threshold;
    }
}
