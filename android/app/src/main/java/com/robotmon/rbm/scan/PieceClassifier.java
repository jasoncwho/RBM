package com.robotmon.rbm.scan;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups detected board pieces into color clusters, one cluster per piece
 * "type". Ported from classifyTsums()/distance3D() in the original script.
 */
public class PieceClassifier {

    private static final double CLUSTER_THRESHOLD = 15;

    public static class Cluster {
        public double sumB, sumG, sumR;
        public double b, g, r;
        public final List<PieceDetector.DetectedPiece> points = new ArrayList<>();
    }

    public List<Cluster> classify(List<PieceDetector.DetectedPiece> points) {
        List<Cluster> clusters = new ArrayList<>();

        for (PieceDetector.DetectedPiece p : points) {
            Cluster best = null;
            double bestDistance = Double.POSITIVE_INFINITY;

            for (Cluster cluster : clusters) {
                double d = colorDistance(cluster.b, cluster.g, cluster.r, p.b, p.g, p.r);
                if (d < CLUSTER_THRESHOLD && d < bestDistance) {
                    bestDistance = d;
                    best = cluster;
                }
            }

            if (best != null) {
                best.points.add(p);
                int count = best.points.size();
                best.sumB += p.b;
                best.sumG += p.g;
                best.sumR += p.r;
                best.b = best.sumB / count;
                best.g = best.sumG / count;
                best.r = best.sumR / count;
            } else {
                Cluster c = new Cluster();
                c.sumB = p.b;
                c.sumG = p.g;
                c.sumR = p.r;
                c.b = p.b;
                c.g = p.g;
                c.r = p.r;
                c.points.add(p);
                clusters.add(c);
            }
        }

        return clusters;
    }

    /** Ported from distance3D(): a color-space distance biased for the game's palette. */
    private double colorDistance(double b1, double g1, double r1, double b2, double g2, double r2) {
        double d = Math.sqrt((b1 - b2) * (b1 - b2) + (g1 - g2) * (g1 - g2) + (r1 - r2) * (r1 - r2));
        if (Math.abs(b1 - b2) < 20) { d -= 10; }
        if (Math.abs(g1 - g2) < 20) { d -= 10; }
        if (r1 < 120 && r2 < 120) { d -= 20; }
        return d;
    }
}
