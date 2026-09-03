package com.robotmon.rbm.model;

import java.util.List;

/**
 * A unit of work queued by the scanner threads and consumed by the
 * SwipeExecutor: either a multi-point drag linking several same-type pieces
 * (ported from Tsum.prototype.linkTsums()) or a single tap popping a bonus
 * bubble (ported from Tsum.prototype.popGameBubbles()).
 */
public class SwipeTask {

    public enum Type { LINK, TAP }

    public final Type type;

    /** Points in real screen coordinates, in the order they should be visited. */
    public final List<Point2D> points;

    /** Duration, in ms, spent moving between each consecutive pair of points. */
    public final int segmentDurationMs;

    public SwipeTask(Type type, List<Point2D> points, int segmentDurationMs) {
        this.type = type;
        this.points = points;
        this.segmentDurationMs = segmentDurationMs;
    }
}
