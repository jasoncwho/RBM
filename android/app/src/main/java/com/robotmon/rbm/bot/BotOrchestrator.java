package com.robotmon.rbm.bot;

import android.util.Log;

import com.robotmon.rbm.RbmApp;
import com.robotmon.rbm.geometry.GameCoordinateMapper;
import com.robotmon.rbm.heart.HeartFarmingController;
import com.robotmon.rbm.heart.RecordStore;
import com.robotmon.rbm.input.InputController;
import com.robotmon.rbm.page.PageDetector;
import com.robotmon.rbm.page.PageNavigator;
import com.robotmon.rbm.skill.SkillController;

/**
 * Top-level driver that wires BotContext up to PageNavigator/SkillController/
 * HeartFarmingController and runs them on a schedule. Ported from the
 * top-level {@code start(settings)}/{@code stop()} functions in index.js,
 * paired with TaskController (see {@link BotScheduler}).
 *
 * <p>Unlike the original -- where taskPlayGameQuick itself drove board
 * scanning/linking/skill-firing in lockstep, once per loop iteration -- board
 * scanning and swipe dispatch already run continuously and independently of
 * this class (ScreenCaptureService -> ScanEngine -> SwipeQueue ->
 * SwipeExecutor). So this class's "play game" task only needs to navigate
 * into a game and, while one is in progress, handle skill firing / bonus
 * bubble clearing / game-over detection -- the parts that a passive scan
 * loop can't do on its own.
 */
public class BotOrchestrator {
    private static final String TAG = "BotOrchestrator";

    private final GameCoordinateMapper mapper;
    private final InputController input;
    private final BotScheduler scheduler = new BotScheduler();

    private volatile BotContext ctx;
    private volatile PageNavigator navigator;
    private volatile SkillController skillController;
    private volatile HeartFarmingController heartController;
    private volatile RecordStore recordStore;

    public BotOrchestrator(GameCoordinateMapper mapper, InputController input) {
        this.mapper = mapper;
        this.input = input;
    }

    public boolean isRunning() {
        BotContext current = ctx;
        return current != null && current.isRunning();
    }

    /** Builds a fresh BotContext + controllers, registers the enabled tasks, and starts the scheduler thread. */
    public synchronized void start(BotSettings settings) {
        if (isRunning()) {
            Log.w(TAG, "start() called while already running; ignoring");
            return;
        }

        PageDetector pageDetector = new PageDetector(RbmApp.getFrameHolder(), mapper);
        BotContext newCtx = new BotContext(mapper, RbmApp.getFrameHolder(), input, pageDetector);
        PageNavigator newNavigator = new PageNavigator(newCtx);
        SkillController newSkillController = new SkillController(newCtx);
        RecordStore newRecordStore = new RecordStore();
        if (settings.recordSender) {
            newRecordStore.load();
        }
        HeartFarmingController newHeartController = new HeartFarmingController(newCtx, newNavigator, newRecordStore);

        applySettings(newCtx, settings);

        ctx = newCtx;
        navigator = newNavigator;
        skillController = newSkillController;
        heartController = newHeartController;
        recordStore = newRecordStore;

        scheduler.removeAllTasks();
        // Direct-play debug mode: do not run mailbox/ranking tasks, because they
        // navigate and tap non-game controls while the board is being tested.
        if (settings.autoPlayGame) {
            scheduler.addTask("taskPlayGameQuick", 3000, () -> taskPlayGameQuick(newCtx, newSkillController));
        }

        RbmApp.getSwipeExecutor().setPostLinkHook(newSkillController::maybeAutoTapSkill);

        scheduler.start();
        Log.i(TAG, "started");
    }

    /** Stops the scheduler and the current run's cooperative sleep loops. Ported from stop(). */
    public synchronized void stop() {
        RbmApp.getSwipeExecutor().setPostLinkHook(null);
        SkillController currentSkillController = skillController;
        if (currentSkillController != null) {
            currentSkillController.shutdown();
        }
        BotContext current = ctx;
        if (current != null) {
            current.stop();
        }
        scheduler.stop();
        RecordStore currentRecordStore = recordStore;
        if (currentRecordStore != null) {
            currentRecordStore.save();
        }
        Log.i(TAG, "stopped");
    }

    private void applySettings(BotContext target, BotSettings settings) {
        target.keepRuby = settings.keepRuby;
        target.claimAllWithoutCoins = settings.claimAllWithoutCoins;
        target.recordReceive = settings.recordSender;
        target.receiveCheckLimit = settings.receiveCheckLimit;
        target.receiveSecondItem = settings.receiveHeartsSkipFirst;
        target.skillType = settings.skillType;
        target.skillLevel = settings.skillLevel;
        target.skillInterval = settings.skillWaitingTimeSec * 1000L;
        target.noSkillLastFeverSec = settings.noSkillLastFeverSec;
        target.skillAutoTap = settings.skillAutoTap;
        target.autoLaunch = settings.autoLaunchApp;
        target.useFan = settings.useFan;
        target.clearBubbles = settings.clearBubbles;
        target.sentToZero = settings.sendHeartsToZeroScore;
        target.sendHeartMaxDuring = BotSettings.minutesToMs(settings.sendHeartsMaxRuntimeMinutes);
        target.scoreItem = settings.bonusScore;
        target.coinItem = settings.bonusCoin;
        target.expItem = settings.bonusExp;
        target.timeItem = settings.bonusTime;
        target.bubbleItem = settings.bonusBubble;
        target.comboItem = settings.bonusCombo;
        target.tsumCount = settings.bonus5to4 ? 4 : 5;
    }

    /**
     * One "get into a game and keep it going" cycle. Ported from
     * Tsum.prototype.taskPlayGameQuick(), minus the scan/link work that now
     * runs independently (see class doc).
     */
    private void taskPlayGameQuick(BotContext ctx, SkillController skillController) {
        // Debug mode: assume Tsum Tsum is already open and skip page detection/navigation.
       // int sinceslastBubbleClear = 0;
       // while (ctx.isRunning()) {
       //     ctx.sleep(300);

        int sinceslastBubbleClear = 0;
        while (ctx.isRunning()) {
            boolean usedSkill = false;
            while (skillController.useSkill()) {
                usedSkill = true;
                if (ctx.clearBubbles) {
                    sinceslastBubbleClear++;
                }
            }
            if (ctx.clearBubbles && sinceslastBubbleClear >= 2) {
                sinceslastBubbleClear = 0;
                skillController.clearAllBubbles(0, 0);
            } else if (!usedSkill) {
                skillController.popGameBubbles();
            }

            String page = ctx.pageDetector.findPage(1, 1500, () -> ctx.setStartupPhase(true));
            if (!"GamePlaying".equals(page) && !"GamePause".equals(page)) {
                Log.i(TAG, "taskPlayGameQuick: game over");
                return;
            }
            ctx.sleep(300);
        }
    }
}