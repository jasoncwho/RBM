package com.robotmon.rbm.scan;

import com.robotmon.rbm.config.GameConfig;
import com.robotmon.rbm.model.BoardPiece;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds chains of same-type board pieces that can be linked with a single
 * drag. Ported from calculatePaths()/calculateNearTsumPaths()/findNearTsum()/
 * getCanonicalPathKey() in the original script.
 */
public class PathFinder {

    public List<List<BoardPiece>> findPaths(List<BoardPiece> board) {
        Map<Integer, List<BoardPiece>> grouped = new LinkedHashMap<>();
        for (BoardPiece piece : board) {
            grouped.computeIfAbsent(piece.typeIndex, k -> new ArrayList<>()).add(piece);
        }

        List<List<BoardPiece>> paths = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (List<BoardPiece> group : grouped.values()) {
            if (group.size() < GameConfig.MIN_GROUP_SIZE) { continue; }

            for (BoardPiece start : group) {
                List<BoardPiece> path = nearestNeighborChain(start, group);
                if (path.size() > 2) {
                    String key = canonicalKey(path);
                    if (seenKeys.add(key)) {
                        paths.add(path);
                    }
                }
            }
        }

        paths.sort((a, b) -> b.size() - a.size());
        return paths;
    }

    /** Ported from calculateNearTsumPaths(): a greedy nearest-neighbor walk capped at MAX_CHAIN. */
    private List<BoardPiece> nearestNeighborChain(BoardPiece start, List<BoardPiece> group) {
        List<BoardPiece> path = new ArrayList<>();
        path.add(start);

        List<BoardPiece> remaining = new ArrayList<>(group);
        remaining.remove(start);

        BoardPiece current = start;
        while (path.size() < GameConfig.MAX_CHAIN) {
            BoardPiece nearest = null;
            double nearestDistSq = Double.POSITIVE_INFINITY;

            for (BoardPiece candidate : remaining) {
                double distSq = current.distanceSquaredTo(candidate);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = candidate;
                }
            }

            if (nearest == null || Math.sqrt(nearestDistSq) > GameConfig.MAX_NEIGHBOR_DIST) {
                break;
            }

            current = nearest;
            remaining.remove(nearest);
            path.add(current);
        }
        return path;
    }

    /** Ported from getCanonicalKey(): dedupes a chain found from either end. */
    private String canonicalKey(List<BoardPiece> path) {
        StringBuilder forward = new StringBuilder();
        for (BoardPiece p : path) {
            forward.append(p.x).append(',').append(p.y).append('-');
        }
        StringBuilder reverse = new StringBuilder();
        for (int i = path.size() - 1; i >= 0; i--) {
            BoardPiece p = path.get(i);
            reverse.append(p.x).append(',').append(p.y).append('-');
        }
        String forwardKey = forward.toString();
        String reverseKey = reverse.toString();
        return forwardKey.compareTo(reverseKey) < 0 ? forwardKey : reverseKey;
    }
}
