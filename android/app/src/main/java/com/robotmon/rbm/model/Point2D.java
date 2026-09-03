package com.robotmon.rbm.model;

/** A simple 2D point, used for both board-local and real-screen coordinates. */
public class Point2D {
    public final double x;
    public final double y;

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** Ported from getDistance() in index.js (kept squared to avoid sqrt in hot loops). */
    public double distanceSquaredTo(Point2D other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return dx * dx + dy * dy;
    }
}
