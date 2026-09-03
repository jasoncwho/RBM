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

/**
 * Skill readiness checks and activation dispatch. Ported from
 * Tsum.prototype.useSkill()/isSkillActive()/checkSkillReadiness()/
 * fanWouldBeWasted()/clearAllBubbles()/popGameBubbles() in index.js.
 */
public class SkillController {
    private final BotContext ctx;
    private final BubbleDetector bubbleDetector = new BubbleDetector();
    private final Map<String, SkillHandler> handlers = new HashMap<>();
    private final SkillHandler defaultHandler = new DefaultSkillHandler();

    /** Bubbles found by the most recent on-demand board scan (e.g., by CptlySkillHandler). Ported from this.gameBubbles. */
    private volatile List<Bubble> gameBubbles = Collections.emptyList();

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
            if (ColorUtils.isSameColor(notActive, c, 25)) { matchesTight = true; }
            if (ColorUtils.isSameColor(notActive, c, 60)) { matchesLoose = true; }
        }
        if (!matchesLoose) { return "active"; }
        if (!matchesTight) { return "almost"; }
        return "far";
    }

    /** Whether firing the fan now would be wasted -- see fanWouldBeWasted() in index.js. */
    public boolean fanWouldBeWasted() {
        Mat img = ctx.frameHolder.getClone();
        if (img == null) {
            return false;
        }
        try {
            return !"far".equals(checkSkillReadiness(img, ButtonCatalog.GAME_SKILL_1));
        } finally {
            img.release();
        }
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
        if ("no_skill".equals(ctx.skillType)) {
            return false;
        }
        String page = ctx.pageDetector.findPage(1, 500, null);
        if (!"GamePlaying".equals(page) && !"GamePause".equals(page)) {
            return false;
        }

        boolean skillActive2 = false;
        for (int i = 0; i < 2; i++) {
            Mat img = ctx.frameHolder.getClone();
            if (img == null) {
                return false;
            }
            boolean skillActive1;
            try {
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
                    ctx.pageDetector.getColor(img, 340, 310), new RgbColor(0, 48, 49), 88);
            HsvColor feverRingLeft = ColorUtils.rgb2hsv(ctx.pageDetector.getColor(img, 332, 1666));
            HsvColor feverRingRight = ColorUtils.rgb2hsv(ctx.pageDetector.getColor(img, 746, 1666));
            double hueDiff = Math.min(
                    Math.abs(feverRingLeft.h - feverRingRight.h),
                    360 - Math.abs(feverRingLeft.h - feverRingRight.h));
            boolean fever2 = hueDiff > 20;
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