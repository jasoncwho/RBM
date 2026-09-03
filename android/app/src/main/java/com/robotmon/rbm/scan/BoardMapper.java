package com.robotmon.rbm.scan;

import com.robotmon.rbm.config.GameConfig;
import com.robotmon.rbm.model.BoardPiece;
import com.robotmon.rbm.model.Bubble;
import com.robotmon.rbm.model.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps coordinates from the resized board-capture frame back to real screen
 * pixels. Ported from Tsum.prototype.toRealXY()/linkTsumS()/popGameBubbles().
 */
public class BoardMapper {
    // private static final int PLAYABLE_MARGIN_PX = 60;
    private final int playOffsetX;
    private final int playOffsetY;
    private final int playWidth;
    private final int playHeight;
    private final int captureWidth;
    private final int captureHeight;

    public BoardMapper(int playOffsetX, int playOffsetY, int playWidth, int playHeight,
                       int captureWidth, int captureHeight) {
        this.playOffsetX = playOffsetX;
        this.playOffsetY = playOffsetY;
        this.playWidth = playWidth;
        this.playHeight = playHeight;
        this.captureWidth = captureWidth;
        this.captureHeight = captureHeight;
    }

    public List<Point2D> toScreenPoints(List<BoardPiece> path) {
        List<Point2D> points = new ArrayList<>(path.size());
        for (BoardPiece piece : path) {
            points.add(toScreenPoint(piece));
        }
        return points;
    }

    public Point2D toScreenPoint(BoardPiece piece) {
        double x = playOffsetX + (piece.x + GameConfig.TSUM_WIDTH / 2.0) * playWidth / captureWidth;
        double y = playOffsetY + (piece.y + GameConfig.TSUM_WIDTH / 2.0) * playHeight / captureHeight;
        return new Point2D(x, y);
    }

    public Point2D toScreenPoint(Bubble bubble) {
        double x = playOffsetX + bubble.x * playWidth / captureWidth;
        double y = playOffsetY + bubble.y * playHeight / captureHeight;
        return new Point2D(x, y);
    }

    /** Rejects any gesture that leaves the square board area; controls are outside this region. */
    public boolean isPlayablePath(List<Point2D> points) {
        if (points.size() < 2) {
            return false;
        }
        double minX = playOffsetX; //+ PLAYABLE_MARGIN_PX;
        double maxX = playOffsetX + playWidth; //- PLAYABLE_MARGIN_PX;
        double minY = playOffsetY ;//+ PLAYABLE_MARGIN_PX;
        double maxY = playOffsetY + playHeight; //- PLAYABLE_MARGIN_PX;
        for (Point2D point : points) {
            if (point.x < minX || point.x > maxX || point.y < minY || point.y > maxY) {
                return false;
            }
        }
        return true;
    }
}