package com.robotmon.rbm.page;

import android.util.Log;

import com.robotmon.rbm.bot.BotContext;
import com.robotmon.rbm.config.ButtonCatalog;
import com.robotmon.rbm.model.ButtonPoint;
import com.robotmon.rbm.model.PageDefinition;
import com.robotmon.rbm.model.Point2D;

/**
 * Menu navigation: getting from wherever the game currently is to a known
 * screen (friends page, the active game board, the tsum collection, the
 * store). Ported from Tsum.prototype.exitUnknownPage()/goFriendPage()/
 * checkGameItem()/goGamePlayingPage()/goTsumSpage()/goTsumSumStorePage() in
 * index.js.
 */
public class PageNavigator {
    private static final String TAG = "PageNavigator";

    private final BotContext ctx;

    public PageNavigator(BotContext ctx) {
        this.ctx = ctx;
    }

    private Point2D real(ButtonPoint p) {
        return ctx.mapper.toRealXY(p.x, p.y);
    }

    private Point2D real(double x, double y) {
        return ctx.mapper.toRealXY(x, y);
    }

    private void tap(ButtonPoint p) {
        ctx.input.tap(real(p));
    }

    private void tap(double x, double y) {
        ctx.input.tap(real(x, y));
    }

    /**
     * No non-root equivalent exists for checking whether the game app is the
     * current foreground activity ([@code dumpsys window] requires shell
     * access this app sandbox doesn't have). The original itself only ever
     * performs this check when ([@code autolunch] is enabled -- which
     * defaults to false -- so this mirrors that default: always "on".
     */
    public boolean isAppOn() {
        if (!ctx.autoLaunch) {
            return true;
        }
        Log.w(TAG, "autoLaunch is enabled but launching/foregrounding the game app is not supported "
                + "without root/shell access; treating app as already foregrounded.");
        return true;
    }

    /** No-op unless autoLaunch is set, matching the original (see {@link #isAppOn()} for why). */
    public void startApp() {
        if (!ctx.autoLaunch) {
            return;
        }
        Log.w(TAG, "autoLaunch is enabled but auto-launching the game app is not supported "
                + "without root/shell access; nothing to do.");
    }

    /**
     * Best-effort recovery from an unrecognized screen. The original used
     * rooted-device key injection (KEYCODE_DPAD_DOWN then KEYCODE_ENTER) to
     * dismiss unexpected system dialogs before falling back to tapping known
     * "cancel/close" buttons; AccessibilityService has no direct key-event
     * injection equivalent, so GLOBAL_ACTION_BACK is used as an approximation.
     */
    public void exitUnknownPage() {
        com.robotmon.rbm.accessibility.RbmAccessibilityService service =
                com.robotmon.rbm.accessibility.RbmAccessibilityService.getInstance();
        if (service != null) {
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
        }
        ctx.sleep(500);
        tap(ButtonCatalog.GAME_QUESTION_CANCEL);
        tap(ButtonCatalog.GAME_QUESTION_CANCEL_2);
        tap(ButtonCatalog.OUT_CLOSE);
        tap(ButtonCatalog.GAME_STOP);
        ctx.sleep(500);
    }

    /** Navigates to FriendPage from wherever the game currently is. */
    public void goFriendPage() {
        while (ctx.isRunning()) {
            if (!isAppOn()) {
                startApp();
            }
            if (ctx.isStartupPhase()) {
                // sleep longer to safely detect new event windows which might initially take longer to load
                ctx.sleep(5000);
            }
            PageDefinition pageObj = ctx.pageDetector.findPageObject(2, 2000, () -> ctx.setStartupPhase(true));
            String page = pageObj != null ? pageObj.name : "unknown";
            Log.i(TAG, "goFriend: " + page);
            if ("FriendPage".equals(page)) {
                // check again with 3 seconds delay (Event notification/page might fly in)
                ctx.sleep(3000);
                page = ctx.pageDetector.findPage(1, 500, () -> ctx.setStartupPhase(true));
                if ("FriendPage".equals(page)) {
                    // sendMoneyInfo() (Telegram messaging integration) intentionally not ported.
                    ctx.setStartupPhase(false);
                    return;
                }
            } else if ("ClosePage".equals(page)) {
                tap(pageObj.back);
                tap(310, 1588 - 140);
            } else if ("unknown".equals(page)) {
                exitUnknownPage();
            } else {
                tap(pageObj.back);
            }
            ctx.sleep(1000);
        }
    }

    /** Toggles the post-game bonus item chips to match the settings in {@link BotContext}. */
    public void checkGameItem() {
        boolean[] isItemsOn = new boolean[]{
                ctx.scoreItem, ctx.coinItem, ctx.expItem, ctx.timeItem,
                ctx.bubbleItem, ctx.tsumCount == 4, ctx.comboItem
        };
        for (int t = 0; t < 3; t++) {
            org.opencv.core.Mat img = ctx.frameHolder.getClone();
            if (img == null) {
                break;
            }
            boolean isChange = false;
            try {
                for (int i = 0; i < ButtonCatalog.OUT_GAME_ITEMS.size(); i++) {
                    ButtonPoint p = ButtonCatalog.OUT_GAME_ITEMS.get(i);
                    com.robotmon.rbm.model.RgbColor c = ctx.pageDetector.getColor(img, p.x, p.y);
                    boolean off = c.b > 128;
                    if (off && isItemsOn[i]) {
                        tap(p);
                        isChange = true;
                        ctx.sleep(500);
                    } else if (!off && !isItemsOn[i]) {
                        tap(p);
                        isChange = true;
                        ctx.sleep(500);
                    }
                }
            } finally {
                img.release();
            }
            if (!isChange) {
                break;
            }
            ctx.sleep(500);
        }
    }

    /** Navigates to (and confirms) an active GamePlaying screen, starting a new game from StartPage if needed. */
    public void goGamePlayingPage() {
        while (ctx.isRunning()) {
            if (!isAppOn()) {
                startApp();
            }
            PageDefinition pageObj = ctx.pageDetector.findPageObject(2, 2000, () -> ctx.setStartupPhase(true));
            String page = pageObj != null ? pageObj.name : "unknown";
            Log.i(TAG, "play: " + page);
            if ("FriendPage".equals(page)) {
                tap(pageObj.next);
                ctx.sleep(3000);
            } else if ("StartPage".equals(page)) {
                ctx.sleep(500);
                checkGameItem();
                // sendMoneyInfo() intentionally not ported.
                tap(ButtonCatalog.OUT_START);
                ctx.sleep(5000); // avoid checking items again!
            } else if ("GamePlaying".equals(page)) {
                // check again
                page = ctx.pageDetector.findPage(1, 500, () -> ctx.setStartupPhase(true));
                if ("GamePlaying".equals(page)) {
                    ctx.setStartupPhase(false);
                    return;
                }
            } else if ("GamePause".equals(page)) {
                ctx.setStartupPhase(false);
                tap(pageObj.next);
                ctx.sleep(500);
            } else if ("unknown".equals(page)) {
                exitUnknownPage();
            } else if ("ClosePage".equals(page)) {
                tap(pageObj.back);
                tap(310, 1588 - 140);
                ctx.sleep(1000);
            }
        }
    }

    /** Navigates to TsumsPage (the tsum collection screen), going through FriendPage first. */
    public void goTsumsPage() {
        if (!ctx.isRunning()) {
            return;
        }
        if (!isAppOn()) {
            startApp();
        }
        goFriendPage();
        while (ctx.isRunning()) {
            PageDefinition page = ctx.pageDetector.findPageObject(2, 2000, () -> ctx.setStartupPhase(true));
            if (page != null) {
                Log.i(TAG, "goTsumPage: " + page.name);
            }
            if (page == null) {
                exitUnknownPage();
            } else if ("TsumsPage".equals(page.name)) {
                // check again
                PageDefinition again = ctx.pageDetector.findPageObject(1, 500, () -> ctx.setStartupPhase(true));
                if (again != null && "TsumsPage".equals(again.name)) {
                    return;
                }
            } else if (page.extra("tsums") != null) {
                tap(page.extra("tsums"));
                ctx.sleep(3000);
            } else {
                tap(page.back);
                ctx.sleep(1000);
            }
        }
    }

    /** Navigates to the TsumTsum store page (used for opening purchased/awarded boxes). */
    public boolean goTsumTsumStorePage() {
        if (!ctx.isRunning()) {
            return false;
        }
        if (!isAppOn()) {
            startApp();
        }
        goTsumsPage();
        for (int i = 0; i < 3; i++) {
            PageDefinition current = ctx.pageDetector.findPageObject();
            if (current == null || current.extra("store") == null) {
                continue;
            }
            tap(current.extra("store"));
            ctx.sleep(3000);
            PageDefinition page = ctx.pageDetector.findPageObject(5, 2000, () -> ctx.setStartupPhase(true));
            String pageName = page != null ? page.name : "unknown";
            Log.i(TAG, "Pg: " + pageName);
            if (page != null && "TsumTsumStorePage".equals(page.name)) {
                org.opencv.core.Mat img = ctx.frameHolder.getClone();
                if (img == null) {
                    return false;
                }
                try {
                    com.robotmon.rbm.model.RgbColor nextColor =
                            ctx.pageDetector.getColor(img, page.next.x, page.next.y);
                    return com.robotmon.rbm.util.ColorUtils.isSameColor(page.next.color, nextColor, 50);
                } finally {
                    img.release();
                }
            }
        }
        Log.w(TAG, "Unexpected page found in goTsumTsumStorePage()");
        return false;
    }
}