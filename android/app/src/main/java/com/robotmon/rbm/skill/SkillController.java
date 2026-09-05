package com.robotmon.rbm.skill;

import com.robotmon.rbm.RbmApp;
import com.robotmon.rbm.bot.BotContext;
import com.robotmon.rbm.config.ButtonCatalog;
import com.robotmon.rbm.model.ButtonPoint;
import com.robotmon.rbm.model.Bubble;
import com.robotmon.rbm.model.HsvColor;
import com.robotmon.rbm.model.Point2D;
import com.robotmon.rbm.model.RgbColor;
import com.robotmon.rbm.scan.BoardMapper;
import com.robotmon.rbm.scan.BubbleDetector;
import com.robotmon.rbm.util.ColorUtils;

import org.opencv.core.Mat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Skill readiness checks and activation dispatch. Ported from
 * * Tsum.prototype.useSkill()/isSkillActive()/checkSkillReadiness()
 * * fanWouldBewasted()/clearAllBubbles()/popGameBubbles() in index.js.
 */
public class SkillController {
    private final BotContext ctx;
    private final BubbleDetector bubbleDetector = new BubbleDetector();
    private final Map<String, SkillHandler> handlers = new HashMap<>();
    private final SkillHandler defaultHandler = new DefaultSkillHandler();

    /** Bubbles found by the most recent on-demand board scan (e.g., by CptlySkillHandler). Ported from this.gameBubbles. */
    private volatile List<Bubble> gameBubbles = Collections.emptyList();

    // Throttle for maybeAutoTapSkill(), ported from this._lastSkillAutoTap.
    private final AtomicLong lastSkillAutoTap = new AtomicLong(0);
    private static final long SKILL_AUTO_TAP_INTERVAL_MS = 500;

    // maybeAutoTapSkill() is called from SwipeExecutor's swipe-consumer thread and
    // must return immediately, so the actual activation (which for some skill
    // types runs several seconds of choreography, e.g. CptlySkillHandler's
    // ctx.sleep(2100)) runs on this dedicated thread instead -- otherwise that
    // whole duration would stall board swiping. autoTapInFlight prevents a second
    // activation from being submitted while one is still running (the 500ms
    // throttle alone isn't enough once a fire can outlast the throttle window).
    private final ExecutorService autoTapExecutor =
        Executors.newSingleThreadExecutor(r -> new Thread(r, "SkillAutoTap"));
    private final AtomicBoolean autoTapInFlight = new AtomicBoolean(false);

    // Non-burst skills read 'active' on the gauge for a while into their own
    // outro animation, well after useSkill() has returned -- see the
    // block_tiara_minnie_plus_s comment in index.js ("the gauge still reads
    // active through the outro"), which JS papers over only for that one skill
    // by always reporting "did not fire". Every non-burst handler here has the
    // same risk (cinderella/cpt_ly/rapunzel's own choreography sleeps aren't
    // guaranteed to outlast the game's own visual outro), so instead of a
    // per-handler hack, once a non-burst fire happens this flag blocks another
    // one until a readiness read actually observes the gauge as not-active --
    // i.e. confirmation the previous fire really drained it -- rather than
    // trusting the very next read.
    private final AtomicBoolean awaitingGaugedrop = new AtomicBoolean(false);

    // Gap between a swipe's touch-up and the auto-tap skill's touch-down. Without
    // it, dispatching a new gesture immediately after the previous one's
    // onCompleted callback can race the game's own input handling of that
    // swipe's last segment (the callback firing doesn't guarantee the app has
    // finished processing the touch stream yet), can leave the swipe
    // looking like it never completed on screen. Not present in the original
    // script -- Auto.js's raw input injection didn't have this gesture-queue
    // handoff, so this has no JS equivalent to port from.
    private static final long SKIL_TAP_SETTLE_MS = 80;

    // Don't know the reason why these are checked instead of the "active skill" colors,
    // but hopefully for a good reason -- ported verbatim from isSkillActive()/checkSkillReadiness().
    private static final RgbColor[] SKILL_NOT_ACTIVE_COLORS = new RgbColor[]{
        new RgbColor(85, 112, 157),
        new RgbColor(72, 139, 181),
        new RgbColor(16, 73, 128),
        new RgbColor(3, 153, 178),
        new RgbColor(33, 215, 255)
    };

    public SkillController(BotContext ctx) {
        this.ctx = ctx;
        handlers.put("block_lukej_s", new LukeSkillHandler());
        handlers.put("block_lightning_mcqueen_plus_s", new LightningMcQueenPlusSkillHandler());
        handlers.put("block_tiara_minnie_plus_s", new TiaraMinniePlusSkillHandler());
        handlers.put("block_donald_s", new DonaldSkillHandler());
        handlers.put("block_donaldx_s", new DonaldSkillHandler());
        handlers.put("block_marie_s", new ClearBubblesSkillHandler(2000L, 50L, null, 0));
        handlers.put("block_missbunny_s", new ClearBubblesSkillHandler(2000L, 50L, null, 0));
        handlers.put("block_rabbit_s", new ClearBubblesSkillHandler(2000L, 50L, null, 0));
        handlers.put("block_moana_s", new ClearBubblesSkillHandler(2500L, 50L, null, 0));
        handlers.put("block_mickeyh2015_s", new ClearBubblesSkillHandler(1500L, 50L, null, 0));
        handlers.put("block_snowwhite_s", new SnowWhiteSkillHandler());
        handlers.put("block_cinderella_s", new CinderellaSkillHandler());
        handlers.put("block_woody2_s", new Woody2SkillHandler());
        handlers.put("block_cabbage_mickey_s", new CabbageMickeySkillHandler());
        handlers.put("block_rapunzel_plus_s", new RapunzelPlusSkillHandler());
        handlers.put("block_cpt_ly_s", new CptLySkillHandler());
    }

    public BotContext getCtx() {
        return ctx;
    }

    public BubbleDetector getBubbleDetector() {
        return bubbleDetector;
    }

    public List<Bubble> getGameBubbles() {
        return gameBubbles;
    }

    public void setGameBubbles(List<Bubble> bubbles) {
        this.gameBubbles = bubbles;
    }

    /** Returns the current BoardMapper, or null if the capture service hasn't started yet. */
    public BoardMapper boardMapper() {
        return RbmApp.getBoardMapper();
    }

    /** Returns a clone of the latest board-play frame (resized to GameConfig.CAPTURE_SIZE); caller must release(). */
    public Mat boardFrame() {
        return RbmApp.getBoardFrameHolder().getClone();
    }

    /** Taps a logical (1080-wide reference) point after converting it to real screen pixels. */
    public void tapLogical(double x, double y, long durationMs) {
        Point2D real = ctx.mapper.toRealXY(x, y);
        ctx.input.tap(real, durationMs);
    }

    public void tapLogical(ButtonPoint p, long durationMs) {
        tapLogical(p.x, p.y, durationMs);
    }

    public void tapLogical(ButtonPoint p) {
        tapLogical(p.x, p.y, 50);
    }

    /** Drags through a sequence of logical points, ported from linkTsums()/linkLongTsums(). */
    public void dragLogicalBoardPoints(List<com.robotmon.rbm.model.BoardPiece> path, long segmentDurationMs) {
        BoardMapper mapper = boardMapper();
        if (mapper == null || path.isEmpty()) {
            return;
        }
        ctx.input.drag(mapper.toScreenPoints(path), segmentDurationMs);
    }

    private boolean isSkillActive(Mat img, ButtonPoint skillButton) {
        RgbColor c = getColor(img, skillButton);
        boolean active = true;
        for (RgbColor notActive : SKILL_NOT_ACTIVE_COLORS) {
            active = active && !ColorUtils.isSameColor(notActive, c, 60);
        }
        return active;
    }

    /**
     * Tiered version of isSkillActive()'s color check: tight (25) means
     * firmly empty, loose (60) is the not-active match. Ported from
     * Tsum.prototype.checkSkillReadiness().
     *
     * @return "active", "almost", or "far"
     */
    public String checkSkillReadiness(Mat img, ButtonPoint skillButton) {
        RgbColor c = getColor(img, skillButton);
        boolean matchesTight = false;
        boolean matchesLoose = false;
        for (RgbColor notActive : SKILL_NOT_ACTIVE_COLORS) {
            if (ColorUtils.isSameColor(notActive, c, 25)) matchesTight = true;
            if (ColorUtils.isSameColor(notActive, c, 60)) matchesLoose = true;
        }
        if (!matchesLoose) return "active";
        if (!matchesTight) return "almost";
        return "far";
    }

    /** Whether firing the fan now would be wasted -- see fanWouldBewasted() in index.js. */
    public boolean fanWouldBewasted() {
        Mat img = ctx.frameHolder.getClone();
        if (img == null) {
            return false;
        }
        try {
            return "far".equals(checkSkillReadiness(img, ButtonCatalog.GAME_SKILL_1));
        } finally {
            img.release();
        }
    }

    /**
     * Called after every swipe dispatched by SwipeExecutor (see
     * SwipeExecutor.setPostLinkHook()). Fires the skill the instant it's
     * ready instead of waiting for the next end-of-cycle useSkill() call.
     * Ported from Tsum.prototype.maybeAutoTapSkill().
     *
     * <p>Must return fast since it runs on SwipeExecutor's swipe-consumer
     * thread: only a cheap throttle/in-flight check happens here (an atomic
     * read or two), and the actual activation -- which for some skill types
     * runs several seconds of choreography -- is handed off to
     * {@link #autoTapExecutor} so it never stalls board swiping.
     */
    public void maybeAutoTapSkill() {
        if (!ctx.skillAutoTap) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = lastSkillAutoTap.get();
        if (now - last < SKILL_AUTO_TAP_INTERVAL_MS) {
            return;
        }
        if (!lastSkillAutoTap.compareAndSet(last, now)) {
            return; // another call already claimed this tick
        }
        // The 500ms throttle above doesn't by itself stop two activations from
        // overlapping (firing can easily outlast 500ms), so guard re-entry too.
        if (!autoTapInFlight.compareAndSet(false, true)) {
            return;
        }
        autoTapExecutor.execute(() -> {
            try {
                fireAutoTapSkill();
            } finally {
                autoTapInFlight.set(false);
            }
        });
    }

    /** The actual auto-tap-skill activation; runs on {@link #autoTapExecutor}, never on the caller's thread. */
    private void fireAutoTapSkill() {
        if ("burst".equals(ctx.skillType) || "burst_bubbles".equals(ctx.skillType)) {
            // A bare tap is a complete activation for burst skills, and it's a
            // no-op while the gauge isn't full -- skip the screenshot entirely.
            // The settle delay keeps this touch-down from landing before the
            // system has finished delivering the just-completed swipe's
            // touch-up to the game, which otherwise can make that swipe's
            // final segment never register on screen.
            ctx.sleep(SKIL_TAP_SETTLE_MS);
            tapLogical(ButtonCatalog.GAME_SKILL_1, 10);
            return;
        }

        // One readiness read before the full useSkill() probe (findPage plus a
        // double gauge check, several screenshots) so the recurring cost while
        // the gauge is still filling stays at a single screenshot.
        Mat img = ctx.frameHolder.getClone();
        if (img == null) {
            return;
        }
        String status;
        try {
            status = checkSkillReadiness(img, ButtonCatalog.GAME_SKILL_1);
        } finally {
            img.release();
        }
        if (awaitingGaugedrop.get()) {
            // Still waiting for confirmation the last fire actually drained the
            // gauge -- only rearm once a read comes back not-active, otherwise
            // this refires on the outro of the very activation it just did.
            if ("active".equals(status)) {
                awaitingGaugedrop.set(false);
            }
            return;
        }
        if (!"active".equals(status)) {
            return;
        }
        // The board's about to change (skill animation covers it, tsums get
        // cleared/rearranged, etc.), so anything SwipeExecutor still has queued
        // was computed against the pre-skill scan -- drop it now rather than
        // dispatch stale touches once useSkill()'s choreography finally returns.
        RbmApp.getSwipeQueue().clear();
        ctx.sleep(SKIL_TAP_SETTLE_MS);
        if (useSkill()) {
            awaitingGaugedrop.set(true);
        }
    }

    /** Stops the background auto-tap-skill thread. Call when the bot stops (see BotOrchestrator.stop()). */
    public void shutdown() {
        autoTapExecutor.shutdownNow();
    }

    private RgbColor getColor(Mat img, ButtonPoint p) {
        return ctx.pageDetector.getColor(img, p.x, p.y);
    }

    /** Ported from Tsum.prototype.clearAllBubbles(): taps a grid across the board to pop bonus bubbles. */
    public void clearAllBubbles(Long startDelay, Long endDelay, Double fromY, Long delayBetweenLines) {
        if (startDelay != null && startDelay > 0) {
            ctx.sleep(startDelay);
        }
        double fy = fromY != null ? fromY : ButtonCatalog.GAME_BUBBLES_FROM.y;
        for (double by = fy; by <= ButtonCatalog.GAME_BUBBLES_TO.y; by += 140) {
            for (double bx = ButtonCatalog.GAME_BUBBLES_FROM.x; bx <= ButtonCatalog.GAME_BUBBLES_TO.x; bx += 140) {
                tapLogical(bx, by, 10);
            }
            ctx.sleep(delayBetweenLines);
        }
        if (endDelay != null && endDelay > 0) {
            ctx.sleep(endDelay);
        }
    }

    public void clearAllBubbles(long startDelay, long endDelay) {
        clearAllBubbles(startDelay, endDelay, null, 0L);
    }

    public void clearAllBubbles() {
        clearAllBubbles(null, null, null, 0L);
    }

    /** Taps the last board-scanned bonus bubbles. Ported from Tsum.prototype.popGameBubbles(). */
    public void popGameBubbles() {
        List<Bubble> bubbles = gameBubbles;
        BoardMapper mapper = boardMapper();
        if (bubbles.isEmpty() || mapper == null) {
            return;
        }
        int count = Math.min(bubbles.size(), com.robotmon.rbm.config.GameBubbleConfig.MAX_TAPS);
        for (int i = 0; i < count; i++) {
            Point2D real = mapper.toScreenPoint(bubbles.get(i));
            ctx.input.tap(real, com.robotmon.rbm.config.GameBubbleConfig.TAP_DURATION_MS);
        }
        gameBubbles = Collections.emptyList();
    }

    /**
     * Checks whether the skill is ready and, if so, runs its choreography.
     * Ported from Tsum.prototype.useSkill(). {code board} from the original
     * signature is unused there too (a vestige) and is dropped here.
     */
      public boolean useSkill() {
        if("no_skill".equals(ctx.skillType)) {
            return false;
        }
        String page = ctx.pageDetector.findPage(1,500,null);
        if(!"GamePlaying".equals(page) && !"GamePause".equals(page)) {
            return false;
        }

        boolean skillActive2 = false;
        for (int i = 0; i < 2; i++) {
            Mat img = ctx.frameHolder.getClone();
            if (img == null) {
                return false;
            }
            boolean skillActive1;
            try{
                skillActive1 = isSkillActive(img, ButtonCatalog.GAME_SKILL_1);
                skillActive2 = "block_pair_tsum".equals(ctx.skillType) && isSkillActive(img, ButtonCatalog.GAME_SKILL_2);
            } finally {
                img.release();
            }

            if (skillActive1 || skillActive2) {
                if (i == 0) {
                    ctx.sleep(200);
                }
            } else {
                return false;
            }
        }

        if (ctx.noSkillLastFeverSec > 0) {
            while (isFeverAlmostOver()) {
                ctx.sleep(100);
            }
        }

        if ("block_lukej_s".equals(ctx.skillType)) {
            tapLogical(ButtonCatalog.SKILL_LUKE_1, 30);
            tapLogical(ButtonCatalog.SKILL_LUKE_2, 30);
            tapLogical(ButtonCatalog.SKILL_LUKE_3, 30);
            tapLogical(ButtonCatalog.SKILL_LUKE_4, 30);
        } else if ("block_lightning_mcqueen_plus_s".equals(ctx.skillType)) {
            ctx.sleep(200); // let tsums settle
        } else if ("block_tiara_minnie_plus_s".equals(ctx.skillType)) {
            TiaraMinniePlusSkillHandler.waitForSettledBoard(this, ctx);
        }

        tapLogical(ButtonCatalog.GAME_SKILL_1, 50);
        ctx.sleep(30);
        if (skillActive2) {
            tapLogical(ButtonCatalog.GAME_SKILL_2, 50);
            ctx.sleep(30);
        }

        SkillHandler handler = handlers.get(ctx.skillType);
        if (handler != null) {
            return handler.run(this, ctx);
        }

        return defaultHandler.run(this, ctx);
    }

    /** Ported from the noSkillLastFeverSec gate inside useSkill(). */
    private boolean isFeverAlmostOver() {
        Mat img = ctx.frameHolder.getClone();
        if (img == null) {
            return false;
        }
        try {
            boolean fever1 = ColorUtils.isSameColor(
                    ctx.pageDetector.getColor(img, 348, 310), new RgbColor(0, 48, 49), 88);
            HsvColor feverRingLeft = ColorUtils.rgb2hsv(ctx.pageDetector.getColor(img, 332, 1666));
            HsvColor feverRingRight = ColorUtils.rgb2hsv(ctx.pageDetector.getColor(img, 746, 1666));
            double hueDiff = Math.min(
                    Math.abs(feverRingLeft.h - feverRingRight.h),
                    360 - Math.abs(feverRingLeft.h - feverRingRight.h));
            boolean fever2 = hueDiff > 28;
            HsvColor feverStart = ColorUtils.rgb2hsv(ctx.pageDetector.getColor(img, 345, 1670));
            int offsetX = (int) Math.floor((733 - 345) * ctx.noSkillLastFeverSec / 10.0);
            HsvColor feverEnd = ColorUtils.rgb2hsv(ctx.pageDetector.getColor(img, 345 + offsetX, 1670));
            boolean feverAlmostOver = feverEnd.v < 90 || Math.abs(feverStart.v - feverEnd.v) > 18;
            RgbColor remainingTimeColor = ctx.pageDetector.getColor(img, 155, 190);
            RgbColor fewSecondsLeftColor = ctx.pageDetector.getColor(img, 144, 195);
            boolean enoughSecondsRemaining = ColorUtils.isSameColor(remainingTimeColor, fewSecondsLeftColor, 60);
            return fever1 && fever2 && feverAlmostOver && enoughSecondsRemaining;
        } finally {
            img.release();
        }
    }
}