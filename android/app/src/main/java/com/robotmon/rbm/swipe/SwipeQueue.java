package com.robotmon.rbm.swipe;

import com.robotmon.rbm.model.SwipeTask;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe hand-off point between the scanner threads (producers, see
 * ScanEngine) and the SwipeExecutor (the single consumer that performs
 * gestures on screen).
 */
public class SwipeQueue {

    // Bounded so a scanner that runs ahead of the swipe consumer cannot pile
    // up stale swipes for a board state that no longer exists by the time
    // they'd be dispatched.
    private static final int CAPACITY = 32;

    private final BlockingQueue<SwipeTask> queue = new LinkedBlockingQueue<>(CAPACITY);

    /** Enqueues a task, dropping it silently if the queue is full rather than blocking the scanner. */
    public void offer(SwipeTask task) {
        queue.offer(task);
    }

    public SwipeTask take() throws InterruptedException {
        return queue.take();
    }

    public SwipeTask poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    /** Clears any pending swipes, e.g. when the board is known to have changed (page transition, game over). */
    public void clear() {
        queue.clear();
    }

    public int size() {
        return queue.size();
    }
}