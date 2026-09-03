package com.robotmon.rbm.config;

/**
 * Bonus-bubble detection constants. Ported verbatim from the
 * {@code GameBubbleConfig} object in the original index.js.
 */
public final class GameBubbleConfig {
    private GameBubbleConfig() {}

    public static final int MIN_RADIUS = 16;
    public static final int MAX_RADIUS = 30;
    public static final int MIN_DIST = 30;
    public static final int PARAM1 = 20;
    public static final int PARAM2 = 26;

    /** Chain length that earns a pop (kept for parity, not enforced here). */
    public static final int MIN_CHAIN_FOR_POP = 4;

    /** Caps how many bubbles get tapped per scan cycle. */
    public static final int MAX_TAPS = 4;

    /** Stroke duration, in ms, used for a bubble tap. */
    public static final int TAP_DURATION_MS = 10;
}
