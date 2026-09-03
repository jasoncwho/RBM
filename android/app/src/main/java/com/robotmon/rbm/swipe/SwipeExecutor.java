package com.robotmon.rbm.swipe;

import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;

import com.robotmon.rbm.accessibility.RbmAccessibilityService;
import com.robotmon.rbm.model.Point2D;
import com.robotmon.rbm.model.SwipeTask;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dedicated consumer thread: takes queued SwipeTasks and performs the actual
 * on-screen tap/drag via the AccessibilityService's gesture dispatcher.
 * Ported from the tapDown()/moveTo()/tapUp() sequence in
 * Tsum.prototype.linkTsums()/popGameBubbles().
 *
 * Gestures are executed one at a time -- a touchscreen cannot process two
 * concurrent strokes -- so this stays single-threaded by design, while the
 * scanning side (ScanEngine) is free to use multiple threads to produce work.
 */
public class SwipeExecutor {
    private static final String TAG = "SwipeExecutor";

    private final SwipeQueue queue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public SwipeExecutor(SwipeQueue queue) {
        this.queue = queue;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            thread = new Thread(this::runLoop, "SwipeExecutor");
            thread.start();
        }
    }

    public void stop() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private void runLoop() {
        while (running.get()) {
            try {
                SwipeTask task = queue.poll(200, TimeUnit.MILLISECONDS);
                if (task == null) { continue; }
                dispatch(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void dispatch(SwipeTask task) {
        RbmAccessibilityService service = RbmAccessibilityService.getInstance();
        if (service == null) {
            Log.w(TAG, "Accessibility service not connected, dropping swipe");
            return;
        }

        List<Point2D> points = task.points;
        if (task.type != SwipeTask.Type.LINK || points.size() < 2) {
            Log.w(TAG, "Ignoring non-swipe gesture");
            return;
        }

        Path path = new Path();
        Point2D first = points.get(0);
        path.moveTo((float) first.x, (float) first.y);
        for (int i = 1; i < points.size(); i++) {
            Point2D p = points.get(i);
            path.lineTo((float) p.x, (float) p.y);
        }

        long totalDuration = Math.max(1L, (long) task.segmentDurationMs * Math.max(1, points.size() - 1));
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, totalDuration);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();

        // Block this thread until the gesture completes (or times out) so the
        // next queued swipe never overlaps the current one on screen.
        boolean completed = service.dispatchGestureBlocking(gesture);
        Log.i(TAG, "link swipe points=" + points.size() + " completed=" + completed);
    }
}