package com.robotmon.rbm.model;

/** h in [0,360), s/v in [0,100]. Ported from rgb2hsv()'s return shape in index.js. */
public class HsvColor {
    public final double h;
    public final double s;
    public final double v;

    public HsvColor(double h, double s, double v) {
        this.h = h;
        this.s = s;
        this.v = v;
    }
}