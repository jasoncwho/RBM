package com.robotmon.rbm.config;

import com.robotmon.rbm.model.ButtonPoint;
import com.robotmon.rbm.model.RgbColor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Ported verbatim from the {@code Button} table in index.js: tap targets and
 * (optionally) the color(s) that should be present at that point before
 * tapping it, in logical 1080-wide reference coordinates.
 *
 * A few entries in the original table are split into a "base" (a bare x or y)
 * and a per-row point, because the script combines them at runtime inside a
 * loop (e.g. iterating received-gift rows). Those are kept as separate
 * constants here (suffixed BASE_X/BASE_Y) rather than folded together, so
 * callers can reproduce the original row math.
 *
 * Not ported (out of scope -- box purchasing / Tsum level unlocking are not
 * automated by this port): moneyInfoBox, outOpenTsumCollectionOrder,
 * outCloseTsumCollectionOrderOld/New, outTsumCollectionOrderBy*Old/New,
 * outTsumCollectionDoUnlock. See README.
 */
public final class ButtonCatalog {
    private ButtonCatalog() {}

    private static RgbColor rgb(int r, int g, int b) {
        return new RgbColor(r, g, b);
    }

    public static final ButtonPoint GAME_BUBBLES_FROM = new ButtonPoint(100, 632);
    public static final ButtonPoint GAME_BUBBLES_TO = new ButtonPoint(1000, 1532);
    public static final ButtonPoint GAME_QUESTION_CANCEL = new ButtonPoint(400, 1352);
    public static final ButtonPoint GAME_QUESTION_CANCEL_2 = new ButtonPoint(400, 1072);
    public static final ButtonPoint GAME_STOP = new ButtonPoint(440, 1072);
    public static final ButtonPoint GAME_SKILL_1 = new ButtonPoint(160, 1702);
    public static final ButtonPoint GAME_SKILL_2 = new ButtonPoint(95, 1702);
    public static final ButtonPoint GAME_RAND = new ButtonPoint(985, 1652, rgb(232, 180, 6));
    public static final ButtonPoint GAME_PAUSE = new ButtonPoint(983, 322, rgb(239, 188, 9));
    public static final ButtonPoint GAME_CONTINUE = new ButtonPoint(540, 1342, rgb(240, 175, 13));

    /** Post-game bonus item chips: +5score, +Coin, +Exp, +Time, +Bubble, 5>4, +Combo. */
    public static final List<ButtonPoint> OUT_GAME_ITEMS = Collections.unmodifiableList(Arrays.asList(
            new ButtonPoint(205, 889),
            new ButtonPoint(435, 893),
            new ButtonPoint(651, 889),
            new ButtonPoint(871, 893),
            new ButtonPoint(201, 1167),
            new ButtonPoint(424, 1170),
            new ButtonPoint(610, 1175)
    ));

    public static final ButtonPoint OUT_START = new ButtonPoint(500, 1592, rgb(236, 111, 129));
    public static final ButtonPoint OUT_CLOSE = new ButtonPoint(500, 1592, rgb(236, 180, 7));
    public static final ButtonPoint OUT_RECEIVE = new ButtonPoint(910, 422);
    public static final ButtonPoint OUT_RECEIVE_ALL = new ButtonPoint(800, 1422);
    public static final ButtonPoint OUT_RECEIVE_OK = new ButtonPoint(835, 1092, rgb(236, 175, 6));
    public static final ButtonPoint OUT_RECEIVE_ALL_HEARTS_DISABLED_JP = new ButtonPoint(679, 880, rgb(41, 129, 214));
    public static final ButtonPoint OUT_RECEIVE_ALL_RUBIES_ENABLED_JP = new ButtonPoint(261, 705, rgb(247, 178, 33));
    public static final ButtonPoint OUT_RECEIVE_ALL_OK_JP = new ButtonPoint(835, 1258, rgb(236, 175, 6));
    public static final ButtonPoint OUT_RECEIVE_ITEM_SET_OK = new ButtonPoint(830, 1260, rgb(238, 176, 8));
    public static final ButtonPoint OUT_RECEIVE_CLOSE = new ButtonPoint(530, 1372);

    /** y-only "base" row anchor; the per-row y is this plus a row offset computed by the caller. */
    public static final double OUT_RECEIVE_ONE_BASE_Y = 569;
    /** x-only entry (paired with {@link #OUT_RECEIVE_ONE_BASE_Y}); y must be supplied by the caller. */
    public static final ButtonPoint OUT_RECEIVE_ONE = new ButtonPoint(840, 0, rgb(235, 181, 30), rgb(40, 74, 119));
    public static final double OUT_RECEIVE_ONE_RUBY_BASE_Y = 651;
    public static final ButtonPoint OUT_RECEIVE_ONE_RUBY = new ButtonPoint(295, 0, rgb(224, 93, 101));
    public static final double OUT_RECEIVE_ONE_AD_BASE_Y = 672;
    public static final ButtonPoint OUT_RECEIVE_ONE_AD = new ButtonPoint(298, 0, rgb(90, 57, 25));

    public static final ButtonPoint OUT_RECEIVE_TIMEOUT = new ButtonPoint(600, 1092, rgb(235, 171, 11));

    public static final ButtonPoint OUT_SEND_HEART_TOP = new ButtonPoint(910, 502);
    public static final ButtonPoint OUT_SEND_HEART_0 = new ButtonPoint(910, 698, rgb(209, 60, 142), rgb(3, 65, 140));
    public static final ButtonPoint OUT_SEND_HEART_1 = new ButtonPoint(910, 895, rgb(209, 60, 142), rgb(3, 65, 140));
    public static final ButtonPoint OUT_SEND_HEART_2 = new ButtonPoint(910, 1102, rgb(209, 60, 142), rgb(3, 65, 140));
    public static final ButtonPoint OUT_SEND_HEART_3 = new ButtonPoint(910, 1304, rgb(209, 60, 142), rgb(3, 65, 140));
    public static final ButtonPoint OUT_SEND_HEART_BOTTOM = new ButtonPoint(910, 1500);
    public static final ButtonPoint OUT_SEND_HEART_CLOSE = new ButtonPoint(666, 1426, rgb(236, 178, 9));
    public static final ButtonPoint OUT_SEND_HEART_FROM = new ButtonPoint(910, 682);
    public static final ButtonPoint OUT_SEND_HEART_TO = new ButtonPoint(910, 1322);
    public static final ButtonPoint OUT_SEND_HEART_END = new ButtonPoint(328, 1266, rgb(47, 85, 132));
    public static final ButtonPoint OUT_SEND_HEART_END_2 = new ButtonPoint(227, 1262, rgb(44, 78, 123));
    public static final ButtonPoint OUT_SEND_HEART_END_3 = new ButtonPoint(316, 1224, rgb(55, 91, 139));

    public static final ButtonPoint OUT_FRIEND_SCORE_FROM = new ButtonPoint(550, 935, rgb(55, 93, 148));
    public static final ButtonPoint OUT_FRIEND_SCORE_TO = new ButtonPoint(760, 935);

    public static final ButtonPoint OUT_HOME_PAGE = new ButtonPoint(60, 1000);
    public static final ButtonPoint OUT_FRIEND_PAGE = new ButtonPoint(60, 1130);

    public static final ButtonPoint SKILL_LUKE_1 = new ButtonPoint(1000, 1372);
    public static final ButtonPoint SKILL_LUKE_2 = new ButtonPoint(830, 1402);
    public static final ButtonPoint SKILL_LUKE_3 = new ButtonPoint(670, 1447);
    public static final ButtonPoint SKILL_LUKE_4 = new ButtonPoint(960, 1232);
    public static final ButtonPoint SKILL_CPT_LY_1 = new ButtonPoint(670, 1050);
    public static final ButtonPoint SKILL_CPT_LY_2 = new ButtonPoint(310, 1050);
    public static final ButtonPoint SKILL_CPT_LY_3 = new ButtonPoint(540, 414);

    public static final double OUT_RECEIVE_NAME_FROM_BASE_Y = 532;
    public static final double OUT_RECEIVE_NAME_FROM_X = 158;
    public static final double OUT_RECEIVE_NAME_TO_BASE_Y = 670;
    public static final double OUT_RECEIVE_NAME_TO_X = 660;
}
