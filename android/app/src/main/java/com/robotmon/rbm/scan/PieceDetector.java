package com.robotmon.rbm.scan;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects circular board pieces ("tsums") in a captured board frame and reads
 * a smoothed BGR color sample at each detected center, so pieces of the same
 * type can later be told apart by color.
 *
 * Ported from findTsums() in the original Auto.js script. The script's HSV
 * conversion (cvtColor(hsvImg, 40)) was dead code there -- only b/g/r
 * channels were ever read back off the "hsv" buffer -- so this keeps the same
 * output using a blurred copy of the BGR frame instead of an actual HSV one.
 */
public class PieceDetector {

    public static class DetectedPiece {
        public final double x;
        public final double y;
        public final double radius;
        public final double b;
        public final double g;
        public final double r;

        DetectedPiece(double x, double y, double radius, double b, double g, double r) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.b = b;
            this.g = g;
            this.r = r;
        }
    }

    private static final double MIN_DIST = 22;
    private static final double PARAM1 = 20;
    private static final double PARAM2 = 10;
    private static final int MIN_RADIUS = 8;
    private static final int MAX_RADIUS = 14;

    /** @param boardBgr the resized play-area frame, in BGR format. */
    public List<DetectedPiece> detect(Mat boardBgr) {
        Mat gray = new Mat();
        Imgproc.cvtColor(boardBgr, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(gray, gray, new Size(9, 9));

        Mat colorSample = new Mat();
        Imgproc.blur(boardBgr, colorSample, new Size(22, 22));

        Mat circles = new Mat();
        Imgproc.HoughCircles(gray, circles, Imgproc.HOUGH_GRADIENT, 1,
                MIN_DIST, PARAM1, PARAM2, MIN_RADIUS, MAX_RADIUS);

        int width = boardBgr.cols();
        int height = boardBgr.rows();

        List<DetectedPiece> results = new ArrayList<>();
        for (int i = 0; i < circles.cols(); i++) {
            double[] c = circles.get(0, i);
            int px = (int) Math.round(c[0]);
            int py = (int) Math.round(c[1]);
            double radius = c[2];

            // Matches findTsums(): sample the center plus its 4 neighbors,
            // falling back to the center value itself for any neighbor that
            // falls off the edge of the frame, then average all 5 samples.
            double[] center = sample(colorSample, px, py);
            double[] left = px - 1 >= 0 ? sample(colorSample, px - 1, py) : center;
            double[] right = px + 1 < width ? sample(colorSample, px + 1, py) : center;
            double[] up = py - 1 >= 0 ? sample(colorSample, px, py - 1) : center;
            double[] down = py + 1 < height ? sample(colorSample, px, py + 1) : center;

            double avgB = (center[0] + left[0] + right[0] + up[0] + down[0]) / 5;
            double avgG = (center[1] + left[1] + right[1] + up[1] + down[1]) / 5;
            double avgR = (center[2] + left[2] + right[2] + up[2] + down[2]) / 5;

            results.add(new DetectedPiece(px, py, radius, avgB, avgG, avgR));
        }

        gray.release();
        colorSample.release();
        circles.release();
        return results;
    }

    private double[] sample(Mat bgr, int x, int y) {
        return bgr.get(y, x);
    }
}