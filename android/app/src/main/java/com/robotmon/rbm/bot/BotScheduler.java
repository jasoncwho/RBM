package com.robotmon.rbm.bot;

import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs a handful of named, recurring tasks on a single dedicated thread, one
 * at a time. Ported from TaskController in index.js: each tick picks the
 * single most "due" task (elapsed time since it last ran &gt; its interval)
 * and runs it, rather than firing every due task concurrently -- the original
 * bot deliberately serializes navigation/skill/heart-farming work so it never
 * fights itself over the same screen.
 *
 * <p>Simplification vs. the original: every real task there was registered
 * with priority 0 (only its internal bookkeeping tasks used a different
 * priority), so the priority tier of getFirstPriorityTaskName() never
 * actually distinguished between user tasks in practice. This drops that
 * always-tied tier and keeps just the tie-breaks that did matter: prefer the
 * task with the longest interval, then the one that has waited longest since
 * it last ran.
 */
public class BotScheduler {

    public interface Task {
        void run();
    }

    private static final String TAG = "BotScheduler";

    /** How often the loop wakes up to check for due tasks; lowered to match the shortest registered task interval. */
    private static final long DEFAULT_TICK_MS = 200;

    private static class TaskEntry {
        final String name;
        final Task task;
        final long intervalMs;
        int runTimesRemaining;
        long lastRunTime;

        TaskEntry(String name, Task task, long intervalMs, int runTimesRemaining, boolean runImmediately) {
            this.name = name;
            this.task = task;
            this.intervalMs = intervalMs;
            this.runTimesRemaining = runTimesRemaining;
            // Matches newTask()'s runNow flag: when set, the first run is delayed
            // by a full interval instead of firing as soon as the loop starts.
            this.lastRunTime = runImmediately ? System.currentTimeMillis() : 0;
        }
    }

    private final Map<String, TaskEntry> tasks = new LinkedHashMap<>();
    private volatile long tickIntervalMs = DEFAULT_TICK_MS;
    private volatile boolean running = false;
    private Thread loopThread;

    /** Registers an infinitely-repeating task, due immediately. */
    public synchronized void addTask(String name, long intervalMs, Task task) {
        addTask(name, intervalMs, task, 0, false);
    }

    /**
     * @param runTimesRemaining how many times to run before auto-removing; 0 means "forever"
     * @param runImmediately   if true, the first run is delayed by a full interval instead of firing right away
     */
    public synchronized void addTask(String name, long intervalMs, Task task, int runTimesRemaining, boolean runImmediately) {
        tasks.put(name, new TaskEntry(name, task, intervalMs, runTimesRemaining, runImmediately));
        if (intervalMs >= 50 && intervalMs < tickIntervalMs) {
            tickIntervalMs = intervalMs;
        }
    }

    public synchronized void removeTask(String name) {
        tasks.remove(name);
    }

    public synchronized void removeAllTasks() {
        tasks.clear();
        tickIntervalMs = DEFAULT_TICK_MS;
    }

    private synchronized TaskEntry pickNext() {
        long now = System.currentTimeMillis();
        TaskEntry best = null;
        for (TaskEntry entry : tasks.values()) {
            if (now - entry.lastRunTime < entry.intervalMs) {
                continue;
            }
            if (best == null || entry.intervalMs > best.intervalMs
                    || (entry.intervalMs == best.intervalMs && entry.lastRunTime < best.lastRunTime)) {
                best = entry;
            }
        }
        return best;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        loopThread = new Thread(this::loop, "BotScheduler");
        loopThread.start();
    }

    public void stop() {
        running = false;
        Thread thread = loopThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void loop() {
        Log.i(TAG, "loop start");
        while (running) {
            TaskEntry entry = pickNext();
            if (entry != null) {
                try {
                    entry.task.run();
                } catch (Exception e) {
                    Log.e(TAG, "Task '" + entry.name + "' failed", e);
                }
                synchronized (this) {
                    entry.lastRunTime = System.currentTimeMillis();
                    if (entry.runTimesRemaining != 0) {
                        entry.runTimesRemaining--;
                        if (entry.runTimesRemaining == 0) {
                            tasks.remove(entry.name);
                        }
                    }
                }
            }
            try {
                Thread.sleep(tickIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        running = false;
        Log.i(TAG, "loop stop");
    }
}