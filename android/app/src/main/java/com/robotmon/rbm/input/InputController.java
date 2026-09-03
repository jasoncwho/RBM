package com.robotmon.rbm.input;

import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;

import com.robotmon.rbm.accessibility.RbmAccessibilityService;
import com.robotmon.rbm.model.Point2D;

import java.util.List;

/**
 * Direct (non-queued) gesture dispatch for page navigation and skill
 * choreography, as opposed to SwipeQueue/SwipeExecutor which serialize board
 * link/tap gestures produced by the scanner.
 *
 * Ported from Tsum.prototype.tap()/tapDown()/moveTo()/tapUp() -- the original
 * issued these as independent, fire-and-forget calls with arbitrary sleeps in
 * between. AccessibilityService instead wants a gesture's whole timeline up
 * front, or (for genuinely incremental touches) chained continueStroke()
 * segments where each must be dispatched only after the previous one
 * completes -- see {@link DragSession}.
 */
public class InputController {
    private static final String TAG = "InputController";

    /** Single tap-and-release at one point. Ported from Tsum.prototype.tap(). */
    public boolean tap(Point2D point, long durationMs) {
        RbmAccessibilityService service = RbmAccessibilityService.getInstance();
        if (service == null) {
            Log.w(TAG, "Accessibility service not connected, dropping tap");
            return false;
        }
        Path path = new Path();
        path.moveTo((float) point.x, (float) point.y);
        long duration = Math.max(1L, durationMs);
        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(path, 0, duration);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        return service.dispatchGestureBlocking(gesture);
    }

    public boolean tap(Point2D point) {
        return tap(point, 50);
    }

    /**
     * One continuous drag through {@code points}, each segment taking
     * {@code segmentDurationMs}. Convenience for simple (no mid-drag holds)
     * multi-point drags, e.g. Woody2/Rapunzel+ style snake drags.
     */
    public boolean drag(List<Point2D> points, long segmentDurationMs) {
        if (points.isEmpty()) {
            return false;
        }
        RbmAccessibilityService service = RbmAccessibilityService.getInstance();
        if (service == null) {
            Log.w(TAG, "Accessibility service not connected, dropping drag");
            return false;
        }
        Path path = new Path();
        Point2D first = points.get(0);
        path.moveTo((float) first.x, (float) first.y);
        for (int i = 1; i < points.size(); i++) {
            Point2D p = points.get(i);
            path.lineTo((float) p.x, (float) p.y);
        }
        long totalDuration = Math.max(1L, segmentDurationMs * Math.max(1, points.size() - 1));
        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(path, 0, totalDuration);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        return service.dispatchGestureBlocking(gesture);
    }

    /** Starts a new incremental drag session (down, moveTo, up), see {@link DragSession}. */
    public DragSession beginDrag(Point2D start) {
        return beginDrag(start, 50);
    }

    /** Like {@link #beginDrag(Point2D)}, matching the original tapDown(xy, during)'s explicit duration. */
    public DragSession beginDrag(Point2D start, long durationMs) {
        DragSession session = new DragSession();
        session.down(start, durationMs);
        return session;
    }

    /**
     * A single continuous on-screen touch built incrementally out of
    * down(), moveTo(), and up() calls, using GestureDescription.StrokeDescription
     * .continueStroke() (API 26+) so the finger genuinely never lifts between
     * segments -- mirroring the original's tapDown()/moveTo()/tapUp() model,
     * which Auto.js could issue as independent raw touch events.
     *
     * Each step blocks until the previous segment's gesture completes, since
     * continueStroke() must be called against an already-dispatched stroke.
     */
    public static class DragSession {
        private static final String TAG = "DragSession";
        private static final long MIN_SEGMENT_MS = 1L;

        private GestureDescription.StrokeDescription lastStroke;
        private Point2D lastPoint;
        private boolean ended;

        void down(Point2D point) {
            down(point, MIN_SEGMENT_MS);
        }

        void down(Point2D point, long durationMs) {
            Path path = new Path();
            path.moveTo((float) point.x, (float) point.y);
            lastStroke = new GestureDescription.StrokeDescription(path, 0, Math.max(MIN_SEGMENT_MS, durationMs), true);
            lastPoint = point;
            dispatch(lastStroke);
        }

        /** Continues the touch to {@code point}, taking {@code durationMs} to get there. */
        public boolean moveTo(Point2D point, long durationMs) {
            if (ended || lastStroke == null) {
                return false;
            }
            Path path = new Path();
            path.moveTo((float) lastPoint.x, (float) lastPoint.y);
            lastStroke = lastStroke.continueStroke(path, 0, Math.max(MIN_SEGMENT_MS, durationMs), true);
            lastPoint = point;
            return dispatch(lastStroke);
        }

        /** Holds in place for {@code durationMs} without moving (still part of the same touch). */
        public boolean hold(long durationMs) {
            if (ended || lastStroke == null) {
                return false;
            }
            Path path = new Path();
            path.moveTo((float) lastPoint.x, (float) lastPoint.y);
            lastStroke = lastStroke.continueStroke(path, 0, Math.max(MIN_SEGMENT_MS, durationMs), true);
            return dispatch(lastStroke);
        }

        /** Lifts the finger, ending the touch. */
        public boolean up(long durationMs) {
            if (ended || lastStroke == null) {
                return false;
            }
            ended = true;
            Path path = new Path();
            path.moveTo((float) lastPoint.x, (float) lastPoint.y);
            lastStroke = lastStroke.continueStroke(path, 0, Math.max(MIN_SEGMENT_MS, durationMs), false);
            return dispatch(lastStroke);
        }

        private boolean dispatch(GestureDescription.StrokeDescription stroke) {
            RbmAccessibilityService service = RbmAccessibilityService.getInstance();
            if (service == null) {
                Log.w(TAG, "Accessibility service not connected, dropping drag segment");
                return false;
            }
            GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
            return service.dispatchGestureBlocking(gesture);
        }
    }
}