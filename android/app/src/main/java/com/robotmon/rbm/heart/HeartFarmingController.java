package com.robotmon.rbm.heart;

import android.util.Log;

import com.robotmon.rbm.bot.BotContext;
import com.robotmon.rbm.config.ButtonCatalog;
import com.robotmon.rbm.config.PageCatalog;
import com.robotmon.rbm.input.InputController;
import com.robotmon.rbm.model.ButtonPoint;
import com.robotmon.rbm.model.PageDefinition;
import com.robotmon.rbm.model.Point2D;
import com.robotmon.rbm.model.RgbColor;
import com.robotmon.rbm.page.PageNavigator;
import com.robotmon.rbm.util.ColorUtils;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * Heart mailbox / heart-sending automation. Ported from
 * Tsum.prototype.taskReceiveAllItems()/fetchAllMails()/taskReceiveOneItem()/
 * skipAd()/friendPageGoToSelf()/doHeartSending()/taskSendHearts()/
 * sendHeart() in index.js.
 *
 * <p>Sender recognition (recognizeSender()) is approximated -- see
 * SenderRecognizer/RecordStore.
 */
public class HeartFarmingController {
    private static final String TAG = "HeartFarming";

    private final BotContext ctx;
    private final PageNavigator navigator;
    private final RecordStore recordStore;

    public HeartFarmingController(BotContext ctx, PageNavigator navigator, RecordStore recordStore) {
        this.ctx = ctx;
        this.navigator = navigator;
        this.recordStore = recordStore;
    }

    private double yOffset() {
        return ctx.receiveSecondItem ? 202 : 0;
    }

    private ButtonPoint receiveOneButton() {
        ButtonPoint b = ButtonCatalog.OUT_RECEIVE_ONE;
        return new ButtonPoint(b.x, ButtonCatalog.OUT_RECEIVE_ONE_BASE_Y + yOffset(), b.color, b.color2);
    }

    private ButtonPoint receiveOneRubyButton() {
        ButtonPoint b = ButtonCatalog.OUT_RECEIVE_ONE_RUBY;
        return new ButtonPoint(b.x, ButtonCatalog.OUT_RECEIVE_ONE_RUBY_BASE_Y + yOffset(), b.color);
    }

    private ButtonPoint receiveOneAdButton() {
        ButtonPoint b = ButtonCatalog.OUT_RECEIVE_ONE_AD;
        return new ButtonPoint(b.x, ButtonCatalog.OUT_RECEIVE_ONE_AD_BASE_Y + yOffset(), b.color);
    }

    private ButtonPoint receiveNameFrom() {
        return new ButtonPoint(ButtonCatalog.OUT_RECEIVE_NAME_FROM_X, ButtonCatalog.OUT_RECEIVE_NAME_FROM_BASE_Y + yOffset());
    }

    private ButtonPoint receiveNameTo() {
        return new ButtonPoint(ButtonCatalog.OUT_RECEIVE_NAME_TO_X, ButtonCatalog.OUT_RECEIVE_NAME_TO_BASE_Y + yOffset());
    }

    private void tap(ButtonPoint p) {
        tap(p, 50);
    }

    private void tap(ButtonPoint p, long durationMs) {
        Point2D real = ctx.mapper.toRealXY(p.x, p.y);
        ctx.input.tap(real, durationMs);
    }

    private void tap(double x, double y) {
        tap(x, y, 50);
    }

    private void tap(double x, double y, long durationMs) {
        Point2D real = ctx.mapper.toRealXY(x, y);
        ctx.input.tap(real, durationMs);
    }

    private RgbColor colorAt(Mat img, ButtonPoint p) {
        return ctx.pageDetector.getColor(img, p.x, p.y);
    }

    /** Receives every mailbox gift in one go. Ported from taskReceiveAllItems(). */
    public void taskReceiveAllItems() {
        if ("GamePause".equals(ctx.pageDetector.findPage())) {
            return;
        }
        navigator.goFriendPage();
        ctx.sleep(1000);
        tap(ButtonCatalog.OUT_RECEIVE);
        ctx.sleep(3500);
        tap(ButtonCatalog.OUT_RECEIVE_ALL);
        ctx.sleep(2500);
        fetchAllMails();
        ctx.sleep(2000);
        tap(ButtonCatalog.OUT_RECEIVE_CLOSE);
        ctx.sleep(1500);
        tap(ButtonCatalog.OUT_CLOSE);
        navigator.goFriendPage();
    }

    /** Dismisses the confirmation dialog that follows "receive all". Ported from fetchAllMails(). */
    public void fetchAllMails() {
        ButtonPoint intOkButton = ButtonCatalog.OUT_RECEIVE_OK;
        ButtonPoint jpOkButton = ButtonCatalog.OUT_RECEIVE_ALL_OK_JP;

        Mat img = ctx.frameHolder.getClone();
        if (img == null) {
            return;
        }
        try {
            if (ctx.pageDetector.isOnScreenshot(img, intOkButton, 35)) {
                tap(intOkButton);
            } else if (ctx.pageDetector.isOnScreenshot(img, jpOkButton, 35)) {
                if (ctx.keepRuby && ctx.pageDetector.isOnScreenshot(img, ButtonCatalog.OUT_RECEIVE_ALL_RUBIES_ENABLED_JP)) {
                    tap(ButtonCatalog.OUT_RECEIVE_ALL_RUBIES_ENABLED_JP);
                    ctx.sleep(500);
                }
                if (ctx.pageDetector.isOnScreenshot(img, ButtonCatalog.OUT_RECEIVE_ALL_HEARTS_DISABLED_JP)) {
                    tap(ButtonCatalog.OUT_RECEIVE_ALL_HEARTS_DISABLED_JP);
                    ctx.sleep(500);
                }
                tap(jpOkButton);
            } else {
                Log.w(TAG, "ERROR! No OK button found!");
                navigator.exitUnknownPage();
            }
        } finally {
            img.release();
        }
    }

    /** Ported from Tsum.prototype.skipAd(): dismisses/ignores an ad shown in place of a gift. */
    public void skipAd() {
        tap(receiveOneButton());
        ctx.sleep(1000);
        // also gets called for skill and premium tickets, so check we really have an ad!!!
        if (ctx.pageDetector.matchesPage("ReceiveSkillTicket") || ctx.pageDetector.matchesPage("ReceivePremiumTicket")) {
            Log.i(TAG, "Receive ticket");
            tap(ButtonCatalog.OUT_RECEIVE_OK);
        } else {
            Log.i(TAG, "Ignore Ad");
            ctx.sleep(4000);
            // delete ad
            tap(462, 1895);
            ctx.sleep(4000);
            tap(172, 1220);
            ctx.sleep(2000);
            tap(556, 1417);
        }
    }

    /** Receives mailbox gifts one at a time, recognizing/recording heart senders. Ported from taskReceiveOneItem(). */
    public void taskReceiveOneItem() {
        if ("GamePause".equals(ctx.pageDetector.findPage())) {
            return;
        }
        navigator.goFriendPage();
        ctx.sleep(1000);
        tap(ButtonCatalog.OUT_RECEIVE);
        ctx.sleep(1000);

        int receivedCount = 0;
        int receiveCheckLimit = 1;
        String sender = null;
        boolean senderPending = false;
        long receiveTime = System.currentTimeMillis();
        int timeoutCounter = 0;
        int maxTimeoutCount = 100;
        int receivedHeartWithoutCoins = 0;

        ButtonPoint receiveOne = receiveOneButton();
        ButtonPoint receiveOneRuby = receiveOneRubyButton();
        ButtonPoint receiveOneAd = receiveOneAdButton();

        while (ctx.isRunning() && timeoutCounter < maxTimeoutCount) {
            Mat img = ctx.frameHolder.getClone();
            if (img == null) {
                ctx.sleep(100);
                continue;
            }
            boolean isItem;
            boolean isRuby;
            boolean isNonItem;
            boolean isAd;
            boolean isOk;
            boolean isOk2;
            boolean isTimeout;
            boolean isHeartWithoutCoins;
            try {
                isItem = ColorUtils.isSameColor(receiveOne.color, colorAt(img, receiveOne), 35);
                isRuby = ColorUtils.isSameColor(receiveOneRuby.color, colorAt(img, receiveOneRuby), 35);
                isNonItem = ColorUtils.isSameColor(receiveOne.color2, colorAt(img, receiveOne), 35);
                isAd = ColorUtils.isSameColor(receiveOneAd.color, colorAt(img, receiveOneAd), 35);
                isOk = ColorUtils.isSameColor(ButtonCatalog.OUT_RECEIVE_OK.color, colorAt(img, ButtonCatalog.OUT_RECEIVE_OK), 35);
                isOk2 = ColorUtils.isSameColor(ButtonCatalog.OUT_RECEIVE_ITEM_SET_OK.color, colorAt(img, ButtonCatalog.OUT_RECEIVE_ITEM_SET_OK), 35);
                isTimeout = ColorUtils.isSameColor(ButtonCatalog.OUT_RECEIVE_TIMEOUT.color, colorAt(img, ButtonCatalog.OUT_RECEIVE_TIMEOUT), 35);
                isHeartWithoutCoins = ctx.pageDetector.matchesPage("ReceiveHeartWithoutCoins");
            } finally {
                img.release();
            }

            if (isItem) {
                if (isAd) {
                    skipAd();
                    ctx.sleep(2000);
                    continue;
                }
                if (receivedHeartWithoutCoins > 2) {
                    if (receivedCount <= 5 + receivedHeartWithoutCoins) {
                        ctx.sleep(2000);
                        taskReceiveAllItems();
                        tap(ButtonCatalog.OUT_RECEIVE);
                        ctx.sleep(1500);
                    }
                    tap(ButtonCatalog.OUT_CLOSE);
                    receivedHeartWithoutCoins = 0;
                    tap(ButtonCatalog.OUT_CLOSE);
                    navigator.goFriendPage();
                    ctx.sleep(500);
                    receivedCount = 0;
                    sender = "";
                    senderPending = true;
                    timeoutCounter = 0;
                    ctx.sleep(500);
                    tap(ButtonCatalog.OUT_RECEIVE);
                    ctx.sleep(1500);
                } else if (!ctx.keepRuby || !isRuby) {
                    if (ctx.recordReceive) {
                        Mat img2 = ctx.frameHolder.getClone();
                        if (img2 != null) {
                            try {
                                boolean isItem2 = ColorUtils.isSameColor(receiveOne.color, colorAt(img2, receiveOne), 30);
                                if (isItem2) {
                                    tap(receiveOne);
                                    sender = recognizeSender(img2);
                                    senderPending = true;
                                }
                            } finally {
                                img2.release();
                            }
                        }
                    } else {
                        sender = "";
                        senderPending = true;
                    }
                    tap(receiveOne);
                    ctx.sleep(200);
                    timeoutCounter = 0;
                } else {
                    isNonItem = true;
                    receiveTime = 0;
                }
            } else if (isTimeout) {
                tap(ButtonCatalog.OUT_RECEIVE_OK);
                ctx.sleep(1000);
                timeoutCounter = 0;
            } else if (isOk || isOk2) {
                if (ctx.recordReceive && sender != null && !sender.isEmpty()) {
                    recordStore.countReceiveHeart(sender);
                    recordStore.save();
                }
                ctx.sleep(100);
                if (isOk) {
                    tap(ButtonCatalog.OUT_RECEIVE_OK);
                } else {
                    tap(ButtonCatalog.OUT_RECEIVE_ITEM_SET_OK);
                }
                if (senderPending) {
                    recordStore.incrementReceivedCount();
                    receivedCount++;
                }
                sender = null;
                senderPending = false;
                timeoutCounter = 0;
                if (ctx.claimAllWithoutCoins && isHeartWithoutCoins) {
                    receivedHeartWithoutCoins++;
                }
            } else {
                    tap(ButtonCatalog.OUT_RECEIVE_CLOSE); // usual close button
                }
                ctx.sleep(200);

            if (!isNonItem) {
                receiveTime = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - receiveTime > 3000) {
                tap(ButtonCatalog.OUT_CLOSE);
                navigator.goFriendPage();
                ctx.sleep(500);
                if (receivedCount == 0 || receiveCheckLimit >= ctx.receiveCheckLimit) {
                    break;
                } else {
                    receiveCheckLimit++;
                    receivedCount = 0;
                    sender = "";
                    senderPending = false;
                    timeoutCounter = 0;
                    ctx.sleep(500);
                    tap(ButtonCatalog.OUT_RECEIVE);
                    ctx.sleep(1500);
                }
            }
            timeoutCounter++;
        }
        if (maxTimeoutCount <= timeoutCounter) {
                // we seem to be trapped, try to exit the trap
                navigator.exitUnknownPage();
                ctx.sleep(1000);
                if ("unknown".equals(ctx.pageDetector.findPage())) {
                    navigator.exitUnknownPage();
                    ctx.sleep(1000);
                }
            }
        }

    /** Crops the sender-name area and matches/registers it. Ported from recognizeSender() (approximated). */
    private String recognizeSender(Mat img) {
        Point2D from = ctx.mapper.toResizeXY(receiveNameFrom().x, receiveNameFrom().y);
        Point2D to = ctx.mapper.toResizeXY(receiveNameTo().x, receiveNameTo().y);
        int x = (int) Math.floor(Math.min(from.x, to.x));
        int y = (int) Math.floor(Math.min(from.y, to.y));
        int w = (int) Math.floor(Math.abs(to.x - from.x));
        int h = (int) Math.floor(Math.abs(to.y - from.y));
        x = Math.max(0, Math.min(x, img.cols() - 1));
        y = Math.max(0, Math.min(y, img.rows() - 1));
        w = Math.max(1, Math.min(w, img.cols() - x));
        h = Math.max(1, Math.min(h, img.rows() - y));
        Mat nameImg = new Mat(img, new Rect(x, y, w, h));
        try {
            return recordStore.recognizeSender(nameImg);
        } finally {
            nameImg.release();
        }
    }

    /** "Scrolls" the friend ranking list back to the player's own row. Ported from friendPageGoToSelf(). */
    public void friendPageGoToSelf() {
        tap(ButtonCatalog.OUT_HOME_PAGE, 100);
        ctx.sleep(2000);
        tap(ButtonCatalog.OUT_FRIEND_PAGE, 100);
        ctx.sleep(2000);
    }

    private void scrollToNextHearts() {
        double x = ButtonCatalog.OUT_SEND_HEART_3.x - 10;
        InputController.DragSession drag;
        if (ctx.sendHeartsDownwards) {
            drag = ctx.input.beginDrag(ctx.mapper.toRealXY(x, ButtonCatalog.OUT_SEND_HEART_3.y), 50);
            drag.moveTo(ctx.mapper.toRealXY(x, ButtonCatalog.OUT_SEND_HEART_3.y), 50);
            drag.moveTo(ctx.mapper.toRealXY(x, ButtonCatalog.OUT_SEND_HEART_2.y), 50);
            drag.moveTo(ctx.mapper.toRealXY(x, ButtonCatalog.OUT_SEND_HEART_1.y), 50);
            drag.moveTo(ctx.mapper.toRealXY(x, ButtonCatalog.OUT_SEND_HEART_0.y), 50);
            drag.moveTo(ctx.mapper.toRealXY(x, ButtonCatalog.OUT_SEND_HEART_TOP.y), 500);
            drag.up(100);
        } else {
            drag = ctx.input.beginDrag(ctx.mapper.toRealXY(x, ButtonCatalog.OUT_SEND_HEART_0.y), 50);
            drag.moveTo(ctx.mapper.toRealXY(x, ButtonCatalog.OUT_SEND_HEART_0.y), 50);
            drag.moveTo(ctx.mapper.toRealXY(x, ButtonCatalog.OUT_SEND_HEART_1.y), 50);
            drag.moveTo(ctx.mapper.toRealXY(x, ButtonCatalog.OUT_SEND_HEART_2.y), 50);
            drag.moveTo(ctx.mapper.toRealXY(x, ButtonCatalog.OUT_SEND_HEART_3.y), 50);
            drag.moveTo(ctx.mapper.toRealXY(x, ButtonCatalog.OUT_SEND_HEART_BOTTOM.y), 500);
            drag.up(100);
        }
    }

    /**
     * One scroll-page's worth of heart sending. Ported from doHeartSending().
     *
     * @return true once the ranking list's top/bottom has been reached and
     * the direction has been flipped for next time, or the max runtime has
     * elapsed; false to be called again immediately.
     */
    public boolean doHeartSending(long startTime) {
        int retry = 0;
        int times = 0;
        double hfx = ButtonCatalog.OUT_SEND_HEART_FROM.x;
        double hfy = ButtonCatalog.OUT_SEND_HEART_FROM.y - 40;
        double hty = ButtonCatalog.OUT_SEND_HEART_TO.y + 30;
        Boolean finished = null;

        while (ctx.isRunning() && finished == null) {
            times++;
            if (times % 15 == 0) {
                navigator.goFriendPage();
            }

            java.util.List<ButtonPoint> heartsPos = new java.util.ArrayList<>();
            Mat img = ctx.frameHolder.getClone();
            if (img == null) {
                ctx.sleep(200);
                continue;
            }
            boolean isOk;
            boolean isZero;
            boolean isNotEnd;
            boolean isEnd1;
            boolean isEnd2;
            boolean isEnd3;
            boolean isTop;
            try {
                isOk = ColorUtils.isSameColor(ButtonCatalog.OUT_RECEIVE_OK.color, colorAt(img, ButtonCatalog.OUT_RECEIVE_OK), 48);
                for (double y = hfy; y < hty; y += 8) {
                    boolean isHs = ColorUtils.isSameColor(ButtonCatalog.OUT_SEND_HEART_0.color, ctx.pageDetector.getColor(img, hfx, y), 48);
                    if (isHs) {
                        heartsPos.add(new ButtonPoint(hfx, y, ButtonCatalog.OUT_SEND_HEART_0.color, ButtonCatalog.OUT_SEND_HEART_0.color2));
                        y += 140;
                    }
                }
                isZero = true;
                double fx = ButtonCatalog.OUT_FRIEND_SCORE_FROM.x;
                double tx = ButtonCatalog.OUT_FRIEND_SCORE_TO.x;
                double sy = heartsPos.isEmpty() ? ButtonCatalog.OUT_FRIEND_SCORE_FROM.y : (heartsPos.get(0).y + 35);
                for (double px = fx; px <= tx; px += 20) {
                    isZero = ColorUtils.isSameColor(ButtonCatalog.OUT_FRIEND_SCORE_FROM.color, ctx.pageDetector.getColor(img, px, sy), 48);
                    if (!isZero) {
                        break;
                    }
                }
                isNotEnd = ColorUtils.isSameColor(ButtonCatalog.OUT_SEND_HEART_END_2.color, ctx.pageDetector.getColor(img, 225, 1056), 48);
                isEnd1 = ColorUtils.isSameColor(new RgbColor(162, 84, 53), ctx.pageDetector.getColor(img, 225, 1056), 48);
                isEnd2 = ColorUtils.isSameColor(ButtonCatalog.OUT_SEND_HEART_END.color, colorAt(img, ButtonCatalog.OUT_SEND_HEART_END), 48);
                isEnd3 = ColorUtils.isSameColor(ButtonCatalog.OUT_SEND_HEART_3.color,
                    ctx.pageDetector.getColor(img, 315, 1828), 48);
                isTop = ColorUtils.isSameColor(new RgbColor(255, 227, 115), ctx.pageDetector.getColor(img, 200, 670), 28);
            } finally {
                img.release();
            }

            boolean isEnd = !isNotEnd && isEnd1 && isEnd2 && isEnd3;

            if (isOk && heartsPos.isEmpty()) {
                tap(ButtonCatalog.OUT_RECEIVE_OK);
            }

            if ((heartsPos.isEmpty() && (isEnd || isTop)) || (!ctx.sentToZero && isZero && !heartsPos.isEmpty())) {
                if (retry < 3) {
                    scrollToNextHearts();
                    retry++;
                    ctx.sleep(1000);
                } else {
                    if (ctx.sendHeartMaxDuring != 0) {
                        ctx.sleep(1000);
                        friendPageGoToSelf();
                    }
                    ctx.sendHeartsDownwards = !ctx.sendHeartsDownwards;
                    // we're finished if we reached the top of the ranking and will send downwards again next run
                    finished = ctx.sendHeartsDownwards;
                }
            } else {
                int rTimes = 0;
                for (ButtonPoint heart : heartsPos) {
                    boolean success = sendHeart(heart);
                    if (!success) {
                        success = sendHeart(heart);
                    }
                    if (success) {
                        rTimes++;
                        recordStore.incrementSentCount();
                    } else {
                        navigator.goFriendPage();
                        ctx.sleep(1000);
                    }
                }
                if (!ctx.isRunning()) {
                    return true; // don't let the surrounding job retry this immediately
                }
                if (!heartsPos.isEmpty() && rTimes == 0) {
                    continue;
                }
                if (ctx.recordReceive && !heartsPos.isEmpty()) {
                    recordStore.save();
                }
                ctx.sleep(250);
                scrollToNextHearts();
                ctx.sleep(400);
                if (ctx.sendHeartMaxDuring != 0 && System.currentTimeMillis() - startTime > ctx.sendHeartMaxDuring) {
                    finished = true;
                }
                if (heartsPos.isEmpty() && isEnd2) {
                    ctx.sleep(700); // end bug
                }
            }
        }
        return finished != null && finished;
    }

    /** Sends hearts to every friend once per ranking screen, scrolling through the whole list. Ported from taskSendHearts(). */
    public void taskSendHearts() {
        if ("GamePause".equals(ctx.pageDetector.findPage())) {
            return;
        }
        navigator.goFriendPage();
        ctx.sleep(1000);
        if (ctx.sendHeartMaxDuring == 0) {
            friendPageGoToSelf();
            tap(0, 0, 20); // avoid overlap between zero score and pointer location
        }

        long startTime = System.currentTimeMillis();
        boolean finished;
        do {
            finished = doHeartSending(startTime);
            ctx.sleep(2000);
            if (!finished) {
                friendPageGoToSelf();
                tap(0, 0, 20);
            }
            ctx.sleep(2000);
        } while (!finished);
    }

    /** Sends a heart to one friend's row, handling the gift/received dialogs that follow. Ported from sendHeart(). */
    public boolean sendHeart(ButtonPoint btn) {
        int unknownCount = 0;
        boolean isGift = false;
        boolean isSent = false;
        while (ctx.isRunning()) {
            String page = ctx.pageDetector.findPage(1, 300, null);
            if ("FriendPage".equals(page)) {
                Mat img = ctx.frameHolder.getClone();
                boolean isSendBtn;
                boolean isSentBtn;
                if (img == null) {
                    unknownCount++;
                } else {
                    try {
                        isSendBtn = ColorUtils.isSameColor(btn.color, colorAt(img, btn), 48);
                        isSentBtn = ColorUtils.isSameColor(btn.color2, colorAt(img, btn), 48);
                    } finally {
                        img.release();
                    }
                    if ((isSendBtn || isSentBtn) && !isGift && !isSent) {
                        tap(btn);
                    } else {
                        unknownCount++;
                    }
                }
            } else if ("GiftHeart".equals(page)) {
                tap(ButtonCatalog.OUT_RECEIVE_OK);
                isGift = true;
            } else if ("Received".equals(page)) {
                ctx.sleep(100);
                tap(ButtonCatalog.OUT_SEND_HEART_CLOSE);
                if (isGift) {
                    isSent = true;
                    ctx.sleep(100);
                    return true;
                }
            } else if ("FriendInfo".equals(page)) {
                PageDefinition friendInfo = PageCatalog.get("FriendInfo");
                if (friendInfo != null && friendInfo.back != null) {
                    tap(friendInfo.back);
                }
            } else if ("ClosePage".equals(page)) {
                PageDefinition closePage = PageCatalog.get("ClosePage");
                if (closePage != null && closePage.back != null) {
                    tap(closePage.back);
                }
                tap(310, 1588 - 140);
            } else {
                unknownCount++;
            }
            if (unknownCount >= 15) {
                return false;
            }
        }
        return false;
    }
}