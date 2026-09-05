package com.robotmon.rbm.bot;

import com.robotmon.rbm.capture.LatestFrameHolder;
import com.robotmon.rbm.geometry.GameCoordinateMapper;
import com.robotmon.rbm.input. InputController;
import com. robotmon.rbm.page. PageDetector;

import java.util.concurrent.atomic.AtomicBoolean;

/**
*Shared mutable state for the navigation/skill/heart-farming layer, analogous
*to the single {@code Tsum} instance ("this") that all of index.js's ported
*prototype methods closed over.
*/
public class BotContext {
    public final GameCoordinateMapper mapper; 
    public final LatestFrameHolder frameHolder;
    public final InputController input;
    public final PageDetector pageDetector;

    private final AtomicBoolean running = new AtomicBoolean(true); 
    private volatile boolean startupPhase = true;

    // --- settings, ported from the settings object passed into start() ---
    public boolean keepRuby = false;
    public boolean sendHeartsDownwards = false;
    public boolean claimAllWithoutCoins = false;
    public boolean recordReceive = false; 
    public int receiveCheckLimit = 30;
    /** Whether the second-item UI variant is in play, shifting the receive-row buttons down by 202px. */ 
    public boolean receiveSecondItem = false; 
    public String skillType = "default"; 
    public int skillLevel = 1;
    public long skillInterval = 0;
    public long noSkillLastFeverSec = 0;
    public boolean useFan = false;
    public boolean clearBubbles = false;
    /** Ported from Tsum.prototype.skillAutoTap */
    public volatile boolean skillAutoTap = true;
    /** Kept false (matches the original's default): no non-root is app foregrounded / launch app" support. See README. */
    public boolean autoLaunch = false;
    /** Whether doHeartSending) keeps scrolling toward zero-score friends instead of stopping once it sees one. */ 
    public boolean sentToZero = false;
    /** Max ms taskSendHearts)/doHeartSending() may run before giving up for this cycle; 0 = unlimited. */
    public long sendHeartMaxDuring = 0;

    // post-game bonus-item toggles, ported from checkGameItem() 's settings.
    public boolean scoreItem = false;
    public boolean coinItem = false;
    public boolean expItem = false;
    public boolean timeItem = false;
    public boolean bubbleItem = false;
    public boolean comboItem = false;
    /** 5 tsum types normally; set to 4 when the "5>4" bonus item toggle is on. */
    public int tsumCount = 5;

    public BotContext (GameCoordinateMapper mapper, LatestFrameHolder frameHolder,
                   InputController input, PageDetector pageDetector) {
    this.mapper = mapper;
    this.frameHolder = frameHolder;
    this.input = input;
    this.pageDetector = pageDetector;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void stop() {
        running.set(false) ;
    }

    public boolean isStartupPhase() {
        return startupPhase;
    }

    public void setStartupPhase(boolean value) {

        startupPhase = value;
    }

    /** Ported from Tsum. prototype.sleep(): cooperative, cancellable delay. */
    public void sleep (long ms) {
        long deadline = System. currentTimeMillis() + ms;
        while (running.get()) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return;
            }
            try {
                Thread.sleep(Math.min(remaining, 100));
                }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}