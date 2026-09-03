package com.robotmon.rbm.model;

/**
 * A single board piece: its color-cluster type and its top-left position in
 * resized-capture pixels. Ported from the {@code {tsumIdx, x, y}} objects
 * built in Tsum.prototype.scanBoardQuick().
 */
public class BoardPiece extends Point2D {
    public final int typeIndex;

    public BoardPiece(int typeIndex, double x, double y) {
        super(x, y);
        this.typeIndex = typeIndex;
    }
}