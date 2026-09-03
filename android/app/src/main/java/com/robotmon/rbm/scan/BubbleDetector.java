package com.robotmon.rbm.scan;

import com.robotmon.rbm.config.GameBubbleConfig;
import com.robotmon.rbm.model.Bubble;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the large bonus "bubbles" scattered on the board. Ported from
 * findGameBubbles() in the original Auto.js script.
 */
public class BubbleDetector {

    /** @param boardBgr the resized play-area frame, in BGR format. */
    public List<Bubble> detect(Mat boardBgr) {
        Mat gray = new Mat();
        Imgproc.cvtColor(boardBgr, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(9, 9), 0);

        Mat circles = new Mat();
        Imgproc.HoughCircles(gray, circles, Imgproc.HOUGH_GRADIENT, 1,
                GameBubbleConfig.MIN_DIST, GameBubbleConfig.PARAM1, GameBubbleConfig.PARAM2,
                GameBubbleConfig.MIN_RADIUS, GameBubbleConfig.MAX_RADIUS);

        List<Bubble> bubbles = new ArrayList<>();
        for (int i = 0; i < circles.cols(); i++) {
            double[] c = circles.get(0, i);
            bubbles.add(new Bubble(c[0], c[1], c[2]));
        }

        gray.release();
        circles.release();
        return bubbles;
    }
}