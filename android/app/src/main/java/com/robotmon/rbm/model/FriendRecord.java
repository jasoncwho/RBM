package com.robotmon.rbm.model;

import java.util.HashMap;
import java.util.Map;

/**
 * One recognized friend's heart-receiving history, ported from the
 * {@code this.record[filename] = {receiveCounts: {}, lastReceiveTime: ...}}
 * entries built in Tsum.prototype.recognizeSender()/countReceiveHeart().
 */
public class FriendRecord {
    /** Hearts received per day, keyed by day number (ms epoch / 86400000, floored). */
    public final Map<Long, Integer> receiveCounts = new HashMap<>();
    public long lastReceiveTime;
    /**
     * Perceptual signature of the cropped player-name image, used to
     * recognize the same friend again. Approximates the original's
     * getIdentityScore() image-identity match (an Auto.js built-in not
     * available here) -- see SenderRecognizer.
     */
    public double[] nameSignature;
}