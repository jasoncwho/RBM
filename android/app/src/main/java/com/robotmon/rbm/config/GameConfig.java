package com.robotmon.rbm.config;

/**
 * Board/chain constants. Ported from the (@code Config) object in the
 * original index.js (values: tsumWidth, maxChain, screenResize,
 * gameContinueDelay).
 */
public final class GameConfig {
    private GameConfig() {}

    /** Logical size (in resized-capture pixels) a board piece occupies. */
    public static final int TSUM_WIDTH = 16;

    /** Maximum number of pieces chained together in a single swipe. */
    public static final int MAX_CHAIN = 15;

    /** A candidate neighbor further than this (resized px) breaks the chain. */
    public static final double MAX_NEIGHBOR_DIST = TSUM_WIDTH * 2.8;

    /** Resolution the play area is resized to before Hough circle detection. */
    public static final int CAPTURE_SIZE = 200;

    /** Minimum pieces of the same color needed before searching for chains. */
    public static final int MIN_GROUP_SIZE = 3;

    /** Only the longest N chains found per scan are linked. */
    public static final int MAX_PATHS_PER_SCAN = 6;

    /**
     * Expected distinct piece types on the board. Color clusters beyond this
     * rank (by member count) are treated as detection noise and ignored, the
     * same way the original capped its loop at {@code this.tsumCount - 1}.
     */
    public static final int EXPECTED_PIECE_TYPES = 5;
}