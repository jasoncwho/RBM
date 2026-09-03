package com.robotmon.rbm.scan;

import com.robotmon.rbm.config.GameBubbleConfig;
import com.robotmon.rbm.config.GameConfig;
import com.robotmon.rbm.model.BoardPiece;
import com.robotmon.rbm.model.Bubble;
import com.robotmon.rbm.model.Point2D;
import com.robotmon.rbm.model.SwipeTask;
import com.robotmon.rbm.swipe.SwipeQueue;

import org.opencv.core.Mat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Runs one scan cycle over a captured board frame:
 * <ol>
 *   <li>Detects pieces and bubbles concurrently on a shared thread pool
 *       (the "multi-threading for scanning the screen" part).</li>
 *   <li>Classifies pieces by color and searches for linkable chains.</li>
 *   <li>Converts the results into SwipeTasks and enqueues them for the
 *       SwipeExecutor to consume (the "points in a queue" part).</li>
 * </ol>
 * Mirrors the scanBoardQuick()/calculatePaths()/link() flow of the original
 * script, minus the page-navigation and skill-choreography logic around it.
 */
public class ScanEngine {
    private static final String TAG = "ScanEngine";

    private final ExecutorService scanPool;
    private final PieceDetector pieceDetector = new PieceDetector();
    private final BubbleDetector bubbleDetector = new BubbleDetector();
    private final PieceClassifier classifier = new PieceClassifier();
    private final PathFinder pathFinder = new PathFinder();
    private final SwipeQueue swipeQueue;
    private final BoardMapper boardMapper;

    public ScanEngine(SwipeQueue swipeQueue, BoardMapper boardMapper) {
        // Two independent detectors run per cycle (pieces + bubbles); size the
        // pool to the device so both can run in parallel without starving it.
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
        this.scanPool = Executors.newFixedThreadPool(threads);
        this.swipeQueue = swipeQueue;
        this.boardMapper = boardMapper;
    }

    /** @param boardFrame the resized play-area frame, in BGR format. Not released by this method. */
    public List<List<Point2D>> scanAndEnqueue(Mat boardFrame) throws InterruptedException {
        Future<List<PieceDetector.DetectedPiece>> pieceFuture =
                scanPool.submit(() -> pieceDetector.detect(boardFrame));
        Future<List<Bubble>> bubbleFuture =
                scanPool.submit(() -> bubbleDetector.detect(boardFrame));

        List<PieceDetector.DetectedPiece> rawPieces;
        List<Bubble> bubbles;
        try {
            rawPieces = pieceFuture.get();
            bubbles = bubbleFuture.get();
        } catch (ExecutionException e) {
            throw new RuntimeException("Board scan failed", e.getCause());
        }

        List<PieceClassifier.Cluster> clusters = classifier.classify(rawPieces);
        clusters.sort((a, b) -> b.points.size() - a.points.size());

        List<BoardPiece> board = new ArrayList<>();
        int typeCount = Math.min(clusters.size(), GameConfig.EXPECTED_PIECE_TYPES);
        for (int typeIdx = 0; typeIdx < typeCount; typeIdx++) {
            for (PieceDetector.DetectedPiece p : clusters.get(typeIdx).points) {
                board.add(new BoardPiece(typeIdx,
                        p.x - GameConfig.TSUM_WIDTH / 2.0,
                        p.y - GameConfig.TSUM_WIDTH / 2.0));
            }
        }

        List<List<BoardPiece>> paths = pathFinder.findPaths(board);
        int pathLimit = Math.min(paths.size(), GameConfig.MAX_PATHS_PER_SCAN);
        List<List<Point2D>> acceptedPaths = new ArrayList<>();
        for (int i = 0; i < pathLimit; i++) {
            List<Point2D> screenPoints = boardMapper.toScreenPoints(paths.get(i));
            if (boardMapper.isPlayablePath(screenPoints)) {
                acceptedPaths.add(screenPoints);
                swipeQueue.offer(new SwipeTask(SwipeTask.Type.LINK, screenPoints, 12));
            }
        }
        Log.i(TAG, "scan pieces=" + rawPieces.size() + " board=" + board.size()
            + " paths=" + paths.size() + " accepted=" + acceptedPaths.size());
        return acceptedPaths;
    }

    public void shutdown() {
        scanPool.shutdownNow();
    }
}