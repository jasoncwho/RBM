package com.robotmon.rbm.model;

/** A tappable / sampled point in logical (1080-wide reference) screen coordinates. Ported from Button.* entries. */
public class ButtonPoint {
    public final double x;
    public final double y;
    public final RgbColor color;
    public final RgbColor color2;

    public ButtonPoint(double x, double y) {
        this(x, y, null, null);
    }

    public ButtonPoint(double x, double y, RgbColor color) {
        this(x, y, color, null);
    }

    public ButtonPoint(double x, double y, RgbColor color, RgbColor color2) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.color2 = color2;
    }
}
