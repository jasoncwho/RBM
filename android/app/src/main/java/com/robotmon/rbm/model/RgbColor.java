package com.robotmon.rbm.model;

/** An RGB color sample, 0..255 per channel. */
public class RgbColor {
    public final int r;
    public final int g;
    public final int b;

    public RgbColor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
}
