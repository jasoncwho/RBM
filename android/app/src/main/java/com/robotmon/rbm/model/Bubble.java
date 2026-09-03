package com.robotmon.rbm.model;

/**
 * A large bonus "bubble" detected on the board. Ported from the
 * {@code {x, y, r}} objects returned by findGameBubbles() in index.js.
 */
public class Bubble extends Point2D {
    public final double radius;

    public Bubble(double x, double y, double radius) {
        super(x, y);
        this.radius = radius;
    }
}
