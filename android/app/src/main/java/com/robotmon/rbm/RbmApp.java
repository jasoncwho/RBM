package com.robotmon.rbm;

import android.app.Application;

import com.robotmon.rbm.bot.BotOrchestrator;
import com.robotmon.rbm.capture.LatestFrameHolder;
import com.robotmon.rbm.geometry.GameCoordinateMapper;
import com.robotmon.rbm.input.InputController;
import com.robotmon.rbm.scan.BoardMapper;
import com.robotmon.rbm.swipe.SwipeExecutor;
import com.robotmon.rbm.swipe.SwipeQueue;

/**
 * Holds the app-wide singletons shared between the activity and the services:
 * the swipe queue/executor (board interaction), the coordinate mapper (device
 * geometry), the latest full-screen menu / board-play frames (page detection
 * and on-demand skill-time board reads), and the bot orchestrator (page
 * navigation/skill/heart-farming scheduler).
 */
public class RbmApp extends Application {
    private static SwipeQueue swipeQueue;
    private static SwipeExecutor swipeExecutor;
    private static GameCoordinateMapper coordinateMapper;
    private static LatestFrameHolder frameHolder;
    private static LatestFrameHolder boardFrameHolder;
    private static volatile BoardMapper boardMapper;
    private static BotOrchestrator botOrchestrator;
    private static RbmApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        System.loadLibrary("opencv_java4");
        instance = this;
        swipeQueue = new SwipeQueue();
        swipeExecutor = new SwipeExecutor(swipeQueue);
        coordinateMapper = new GameCoordinateMapper();
        frameHolder = new LatestFrameHolder();
        boardFrameHolder = new LatestFrameHolder();
        botOrchestrator = new BotOrchestrator(coordinateMapper, new InputController());
    }

    /** The singleton Application instance, usable as a Context (e.g. for file storage). */
    public static RbmApp getInstance() {
        return instance;
    }

    public static SwipeQueue getSwipeQueue() {
        return swipeQueue;
    }

    public static SwipeExecutor getSwipeExecutor() {
        return swipeExecutor;
    }

    public static GameCoordinateMapper getCoordinateMapper() {
        return coordinateMapper;
    }

    /** Latest full-screen menu frame, downscaled by resizeRatio -- feeds PageDetector. */
    public static LatestFrameHolder getFrameHolder() {
        return frameHolder;
    }

    /** Latest play-area frame, resized to GameConfig.CAPTURE_SIZE -- feeds on-demand skill board reads. */
    public static LatestFrameHolder getBoardFrameHolder() {
        return boardFrameHolder;
    }

    public static BoardMapper getBoardMapper() {
        return boardMapper;
    }

    public static void setBoardMapper(BoardMapper mapper) {
        boardMapper = mapper;
    }

    /** Drives PageNavigator/SkillController/HeartFarmingController on a schedule once capture/input are up. */
    public static BotOrchestrator getBotOrchestrator() {
        return botOrchestrator;
    }
}