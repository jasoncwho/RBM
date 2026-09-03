package com.robotmon.rbm.heart;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Approximate replacement for the original's getIdentityScore()-based sender
 * recognition (an Auto.js built-in image-identity matcher not available on
 * plain Android/OpenCV). Reduces a cropped player-name image to a small,
 * fixed-size grayscale signature and compares signatures by mean absolute
 * difference; this is a coarse approximation, not a faithful port -- treat
 * matches as best-effort. See Tsum.prototype.recognizeSender() in index.js.
 */
public final class SenderRecognizer {
    private static final int SIG_WIDTH = 32;
    private static final int SIG_HEIGHT = 12;

    /** Below this normalized distance, two signatures are considered the same friend. */
    public static final double MATCH_THRESHOLD = 0.08;

    private SenderRecognizer() {}

    /** Builds a small grayscale signature (values normalized to [0,1]) from a cropped name image. */
    public static double[] signature(Mat nameImg) {
        Mat gray = new Mat();
        Mat resized = new Mat();
        try {
            if (nameImg.channels() > 1) {
                Imgproc.cvtColor(nameImg, gray, Imgproc.COLOR_BGR2GRAY);
            } else {
                nameImg.copyTo(gray);
            }
            Imgproc.resize(gray, resized, new Size(SIG_WIDTH, SIG_HEIGHT));
            double[] out = new double[SIG_WIDTH * SIG_HEIGHT];
            int i = 0;
            for (int y = 0; y < SIG_HEIGHT; y++) {
                for (int x = 0; x < SIG_WIDTH; x++) {
                    out[i++] = resized.get(y, x)[0] / 255.0;
                }
            }
            return out;
        } finally {
            gray.release();
            resized.release();
        }
    }

    /** Mean absolute difference between two signatures, in [0,1]. */
    public static double distance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }
        return sum / a.length;
    }
}