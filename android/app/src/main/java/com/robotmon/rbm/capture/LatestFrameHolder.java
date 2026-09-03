package com.robotmon.rbm.capture;

import org.opencv.core.Mat;

/**
 * Thread-safe holder for the most recent full-screen "menu" screenshot: a BGR
 * Mat downscaled by (@code GameCoordinateMapper.getResizerRatio()}, matching
 * the image produced by Tsum.prototype.screenshot() in index.js. This is the
 * image PageDetector reads pixel colors from to recognize the current screen.
 *
 * Distinct from the board-cropped capture pipeline (ScreenCaptureService ->
 * ScanEngine), which only carries the game-play area used for tsum scanning.
 */
public class LatestFrameHolder {
    private final Object lock = new Object();
    private Mat latestFrame;

    /** Stores a clone of {@code frame}; caller retains ownership of {@code frame}. */
    public void set(Mat frame) {
        synchronized (lock) {
            if (latestFrame != null) {
                latestFrame.release();
            }
            latestFrame = frame.clone();
        }
    }

    /** Returns a clone of the latest frame, or null if none has been captured yet. Caller must release() it. */
    public Mat getClone() {
        synchronized (lock) {
            return latestFrame != null ? latestFrame.clone() : null;
        }
    }

    public void clear() {
        synchronized (lock) {
            if (latestFrame != null) {
                latestFrame.release();
                latestFrame = null;
            }
        }
    }
}