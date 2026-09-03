package com.robotmon.rbm.config;

import com.robotmon.rbm.model.ButtonPoint;
import com.robotmon.rbm.model.PageColorSample;
import com.robotmon.rbm.model.PageDefinition;
import com.robotmon.rbm.model.RgbColor;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported verbatim from the {@code Page} table in index.js: a per-screen set of
 * pixel-color signatures (in logical 1080-wide reference coordinates) used to
 * recognize which screen the game is currently showing.
 *
 * These are the "per device pixel tables" -- they were captured against a
 * 1080x1920 reference device/game version and are read through
 * {@link com.robotmon.rbm.geometry.GameCoordinateMapper}, which rescales them
 * to the actual device. If your device/game version renders differently,
 * individual samples may need recapturing (see the README).
 */
public final class PageCatalog {
    private PageCatalog() {}

    private static PageColorSample s(int x, int y, int r, int g, int b, boolean match, int threshold) {
        return new PageColorSample(x, y, r, g, b, match, threshold);
    }

    private static List<PageColorSample> colors(PageColorSample... items) {
        return Arrays.asList(items);
    }

    private static ButtonPoint bp(double x, double y) {
        return new ButtonPoint(x, y);
    }

    private static ButtonPoint bp(double x, double y, int r, int g, int b) {
        return new ButtonPoint(x, y, new RgbColor(r, g, b));
    }

    private static Map<String, ButtonPoint> extra(String key, ButtonPoint value) {
        Map<String, ButtonPoint> m = new LinkedHashMap<>();
        m.put(key, value);
        return m;
    }

    private static final String SWITCH_TO_STARTUP_MODE = "switchToStartupMode";

    private static final Map<String, PageDefinition> PAGES = new LinkedHashMap<>();

    private static void add(String key, String name, List<PageColorSample> colors,
                          ButtonPoint back, ButtonPoint next) {
        PAGES.put(key, new PageDefinition(key, name, colors, back, next));
    }

    private static void add(String key, String name, List<PageColorSample> colors,
                          ButtonPoint back, ButtonPoint next, Map<String, ButtonPoint> extras) {
        PAGES.put(key, new PageDefinition(key, name, colors, back, next, extras, null));
    }

    private static void add(String key, String name, List<PageColorSample> colors,
                          ButtonPoint back, ButtonPoint next, String onDetectAction) {
        PAGES.put(key, new PageDefinition(key, name, colors, back, next, Collections.emptyMap(), onDetectAction));
    }

        private static void addOnDetect(String key, String name, List<PageColorSample> colors,
                                                                        ButtonPoint back, ButtonPoint next, String onDetectAction) {
                add(key, name, colors, back, next, onDetectAction);
        }

    static {
        add("TodayMissions", "TodayMissions", colors(
                s(764, 445, 248, 190, 15, true, 80),
                s(781, 436, 165, 92, 63, true, 80),
                s(823, 445, 248, 249, 249, true, 80),
                s(554, 444, 45, 111, 142, true, 80),
                s(550, 1421, 33, 196, 231, true, 80),
                s(593, 1423, 240, 175, 8, true, 80),
                s(176, 1658, 238, 172, 8, true, 80),
                s(55, 1649, 238, 172, 8, true, 80),
                s(25, 1655, 8, 16, 26, true, 80)
        ), bp(176, 1662), bp(176, 1662));

        add("TodayMissions", "TodayMission", colors(
                s(540, 1480, 238, 181, 12, true, 80),
                s(975, 500, 161, 224, 231, true, 80),
                s(554, 1332, 24, 189, 219, true, 80)
        ), bp(558, 1473), bp(558, 1473));

        add("ScorePage", "ScorePage", colors(
                s(302, 1581, 235, 184, 7, true, 80),
                s(777, 1588, 248, 142, 20, true, 80),
                s(774, 500, 243, 248, 242, true, 80)
        ), bp(389, 1653), bp(784, 1653));

        add("ProfilePageJp", "ProfilePage", colors(
                s(540, 1592, 246, 135, 17, true, 80),
                s(187, 1599, 240, 218, 72, true, 80),
                s(799, 1653, 232, 170, 7, true, 80),
                s(698, 464, 244, 249, 243, true, 80),
                s(34, 1004, 247, 178, 8, true, 80),
                s(6, 1120, 46, 135, 232, true, 80),
                s(6, 1270, 44, 134, 233, true, 80)
        ), bp(31, 1126), bp(31, 1126), extra("tsums", bp(900, 1653)));

        add("ProfilePageIntl", "ProfilePage", colors(
                s(540, 1592, 246, 135, 17, true, 80),
                s(187, 1599, 240, 218, 72, true, 80),
                s(799, 1653, 232, 170, 7, true, 80),
                s(698, 464, 244, 249, 243, true, 80),
                s(34, 1004, 247, 178, 8, true, 80),
                s(6, 1120, 46, 135, 232, true, 80),
                s(6, 1270, 52, 98, 143, true, 80)
        ), bp(31, 1126), bp(31, 1126), extra("tsums", bp(900, 1653)));

        add("SquarePage", "SquarePage", colors(
                s(540, 1592, 246, 135, 17, true, 80),
                s(187, 1599, 240, 218, 72, true, 80),
                s(799, 1653, 232, 170, 7, true, 80),
                s(18, 994, 46, 135, 234, true, 80),
                s(16, 1120, 46, 135, 232, true, 80),
                s(34, 1270, 247, 175, 8, true, 80)
        ), bp(31, 1126), bp(31, 1126));

        add("FriendPage", "FriendPage", colors(
                s(540, 1592, 246, 135, 17, true, 80),
                s(187, 1599, 240, 218, 72, true, 80),
                s(799, 1653, 232, 170, 7, true, 80),
                s(698, 464, 244, 249, 243, true, 80),
                s(960, 430, 24, 192, 231, true, 80)
        ), bp(547, 1653), bp(547, 1653), extra("tsums", bp(900, 1653)));

        add("FriendPage2", "FriendPage", colors(
                s(540, 1649, 175, 188, 197, true, 80),
                s(187, 1599, 240, 218, 72, true, 80),
                s(799, 1653, 232, 170, 7, true, 80),
                s(698, 464, 244, 249, 243, true, 80),
                s(960, 430, 24, 192, 231, true, 80)
        ), bp(547, 1653), bp(547, 1653), extra("tsums", bp(900, 1653)));

        add("FriendPage3", "FriendPage", colors(
                s(540, 1649, 203, 192, 237, true, 80),
                s(187, 1599, 240, 218, 72, true, 80),
                s(799, 1653, 232, 170, 7, true, 80),
                s(698, 464, 244, 249, 243, true, 80),
                s(960, 430, 24, 192, 231, true, 80)
        ), bp(547, 1653), bp(547, 1653), extra("tsums", bp(900, 1653)));

        add("FriendPage4", "FriendPage", colors(
                s(540, 1649, 79, 89, 94, true, 80),
                s(187, 1599, 240, 218, 72, true, 80),
                s(799, 1653, 232, 170, 7, true, 80),
                s(698, 464, 244, 249, 243, true, 80),
                s(960, 430, 24, 192, 231, true, 80)
        ), bp(547, 1653), bp(547, 1653), extra("tsums", bp(900, 1653)));

        add("GiftHeart", "GiftHeart", colors(
                s(216, 1084, 233, 172, 6, true, 80),
                s(673, 1080, 235, 174, 8, true, 80),
                s(468, 803, 214, 61, 143, true, 100),
                s(572, 561, 30, 193, 224, true, 80),
                s(583, 1195, 28, 186, 221, true, 80)
        ), bp(774, 1095), bp(320, 1091));

        add("MailBox", "MailBox", colors(
                s(738, 414, 240, 245, 239, true, 80),
                s(550, 1581, 238, 187, 10, true, 80),
                s(604, 1419, 234, 171, 6, true, 80)
        ), bp(561, 1653), bp(561, 1653));

        add("MailBox2", "MailBox", colors(
                s(738, 414, 240, 245, 239, true, 80),
                s(550, 1581, 238, 187, 10, true, 80),
                s(619, 1426, 19, 137, 175, true, 80)
        ), bp(561, 1653), bp(561, 1653));

        add("ReceiveHeart", "ReceiveHeart", colors(
                s(208, 1080, 233, 172, 6, true, 80),
                s(662, 1080, 232, 171, 5, true, 80),
                s(561, 554, 28, 191, 222, true, 80),
                s(565, 1210, 30, 195, 225, true, 80),
                s(334, 817, 213, 62, 143, true, 90),
                s(586, 821, 248, 249, 51, true, 100)
        ), bp(774, 1095), bp(320, 1091));

        add("Received", "Received", colors(
                s(799, 716, 30, 188, 223, true, 80),
                s(806, 889, 45, 80, 122, true, 80),
                s(799, 1048, 27, 188, 217, true, 80)
        ), bp(774, 1095), bp(320, 1091));

        add("Received2", "Received", colors(
                s(799, 716, 30, 188, 223, true, 80),
                s(889, 824, 40, 72, 111, true, 80),
                s(799, 1048, 27, 188, 217, true, 80)
        ), bp(774, 1095), bp(320, 1091));

        add("StartPage", "StartPage", colors(
                s(752, 471, 244, 249, 243, true, 80),
                s(856, 1430, 30, 193, 224, true, 80),
                s(169, 1581, 239, 188, 11, true, 80),
                s(547, 1581, 235, 118, 134, true, 80),
                s(792, 1660, 234, 171, 8, true, 100)
        ), bp(190, 1646), bp(558, 1635), extra("tsums", bp(900, 1653)));

        add("StartPage2", "StartPage", colors(
                s(820, 515, 245, 250, 244, true, 80),
                s(954, 1426, 31, 190, 220, true, 80),
                s(180, 1584, 235, 182, 8, true, 80),
                s(540, 1584, 238, 115, 133, true, 80),
                s(1011, 1675, 229, 166, 11, true, 100)
        ), bp(190, 1646), bp(558, 1635));

        add("StartPage3", "StartPage", colors(
                s(400, 1672, 245, 85, 115, true, 80),
                s(680, 1672, 245, 85, 115, true, 80),
                s(540, 1722, 235, 70, 90, true, 80)
        ), bp(190, 1646), bp(558, 1635));

        add("TsumsPage", "TsumsPage", colors(
                s(27, 901, 197, 243, 254, true, 80),
                s(436, 902, 247, 247, 247, true, 80),
                s(713, 900, 247, 187, 16, true, 80),
                s(1030, 900, 249, 190, 19, true, 80)
        ), bp(176, 1592), bp(176, 1592), extra("store", bp(910, 1592)));

        add("TsumTsum2025StorePage", "TsumTsumStorePage", colors(
                s(30, 910, 16, 53, 93, true, 30),
                s(60, 910, 233, 171, 8, true, 30),
                s(520, 910, 237, 174, 8, true, 30),
                s(545, 840, 22, 65, 107, true, 30),
                s(570, 910, 29, 85, 159, true, 30),
                s(10, 955, 37, 71, 115, true, 30),
                s(170, 1490, 48, 81, 130, true, 30),
                s(170, 1515, 8, 164, 213, true, 30),
                s(170, 1570, 247, 194, 16, true, 30)
        ), bp(190, 1650), bp(1000, 690, 238, 172, 8));

        add("ConfirmPurchaseBoxPage", "ConfirmPurchasePage", colors(
                s(208, 1070, 247, 176, 8, true, 30),
                s(420, 1070, 247, 176, 8, true, 30),
                s(540, 1070, 54, 93, 146, true, 30),
                s(650, 1070, 247, 176, 8, true, 30),
                s(880, 1070, 247, 176, 8, true, 30),
                s(948, 1066, 33, 69, 107, true, 30),
                s(805, 1265, 239, 167, 8, true, 50)
        ), bp(310, 1070), bp(760, 1070));

        add("Confirm2025PurchaseBoxPage", "ConfirmPurchasePage", colors(
                s(208, 1070, 247, 186, 8, true, 30),
                s(420, 1070, 247, 184, 8, true, 30),
                s(540, 1070, 54, 90, 141, true, 30),
                s(650, 1070, 247, 190, 8, true, 30),
                s(880, 1070, 247, 191, 14, true, 30),
                s(948, 1066, 40, 70, 113, true, 30),
                s(785, 1320, 238, 171, 8, true, 50)
        ), bp(310, 1070), bp(760, 1070));

        add("ConfirmPurchaseCapsulePage", "ConfirmPurchasePage", colors(
                s(200, 1444, 247, 178, 8, true, 30),
                s(426, 1444, 247, 178, 8, true, 30),
                s(540, 1444, 54, 93, 146, true, 30),
                s(660, 1444, 247, 174, 8, true, 30),
                s(860, 1444, 247, 178, 8, true, 30),
                s(940, 1444, 33, 65, 107, true, 30),
                s(416, 790, 239, 28, 49, true, 30)
        ), bp(320, 1444), bp(766, 1444));

        add("Confirm2025PurchaseCapsulePage", "ConfirmPurchasePage", colors(
                s(200, 1464, 247, 178, 8, true, 30),
                s(426, 1464, 247, 178, 8, true, 30),
                s(540, 1464, 54, 93, 146, true, 30),
                s(660, 1464, 247, 174, 8, true, 30),
                s(860, 1464, 247, 178, 8, true, 30),
                s(940, 1464, 33, 65, 107, true, 30),
                s(836, 1152, 255, 255, 255, true, 30),
                s(860, 1081, 255, 255, 255, true, 30),
                s(860, 1152, 48, 81, 127, true, 30)
        ), bp(320, 1464), bp(766, 1464));

        add("TapOpenPageBox", "TapOpenPage", colors(
                s(641, 328, 255, 255, 231, true, 30),
                s(641, 243, 255, 255, 247, true, 30),
                s(180, 520, 247, 182, 189, true, 30),
                s(899, 777, 140, 121, 156, true, 30),
                s(68, 1265, 33, 73, 107, true, 30),
                s(964, 1265, 33, 73, 115, true, 30),
                s(534, 1840, 33, 190, 231, true, 30)
        ), bp(500, 1600), bp(500, 1600));

        add("TapOpenPageCapsule", "TapOpenPage", colors(
                s(70, 560, 24, 85, 132, true, 30),
                s(899, 777, 137, 117, 148, true, 30),
                s(68, 1265, 33, 73, 107, true, 30),
                s(964, 1265, 33, 73, 115, true, 30),
                s(405, 1397, 255, 255, 255, true, 30),
                s(546, 1429, 255, 255, 255, true, 30),
                s(664, 1407, 255, 255, 255, true, 30),
                s(789, 1381, 255, 255, 255, true, 30)
        ), bp(500, 1600), bp(500, 1600));

        add("TapOpenPageCapsuleDeprecated", "TapOpenPageDeprecated", colors(
                s(620, 328, 205, 13, 34, true, 30),
                s(641, 243, 146, 0, 0, true, 30),
                s(70, 560, 24, 85, 132, true, 30),
                s(899, 777, 137, 117, 148, true, 30),
                s(68, 1265, 33, 73, 107, true, 30),
                s(964, 1265, 33, 73, 115, true, 30),
                s(534, 1840, 33, 190, 231, true, 30)
        ), bp(500, 1600), bp(500, 1600));

        add("BoxPurchasedPage", "BoxPurchasedPage", colors(
                s(156, 1077, 33, 195, 231, true, 30),
                s(48, 998, 24, 52, 82, true, 30),
                s(131, 1134, 33, 65, 107, true, 30),
                s(928, 1077, 33, 203, 239, true, 30),
                s(923, 1183, 33, 65, 107, true, 30),
                s(904, 1396, 33, 199, 239, true, 30),
                s(389, 1634, 247, 174, 8, true, 30),
                s(279, 1627, 41, 77, 115, true, 30),
                s(525, 1823, 24, 158, 189, true, 30)
        ), bp(550, 1630), bp(550, 1630));

        add("PremiumPlusBoxPurchasedPage", "BoxPurchasedPage", colors(
                s(156, 1077, 33, 195, 231, true, 30),
                s(48, 998, 33, 66, 99, true, 30),
                s(131, 1137, 33, 62, 101, true, 30),
                s(928, 1075, 33, 203, 236, true, 30),
                s(922, 1184, 33, 65, 107, true, 30),
                s(904, 1396, 33, 199, 239, true, 30),
                s(389, 1634, 238, 174, 8, true, 30),
                s(280, 1626, 63, 103, 147, true, 30),
                s(525, 1823, 40, 210, 247, true, 30)
        ), bp(550, 1630), bp(550, 1630));

        add("GamePause", "GamePause", colors(
                s(165, 1077, 234, 173, 7, true, 80),
                s(586, 1080, 239, 174, 7, true, 80),
                s(367, 774, 24, 191, 225, true, 80),
                s(738, 612, 248, 244, 245, true, 80),
                s(550, 1336, 247, 185, 8, true, 80)
        ), bp(331, 1080), bp(561, 1422));

        add("GamePlaying480x800", "GamePlaying", colors(
                s(916, 198, 253, 216, 0, true, 80),
                s(916, 318, 241, 161, 8, true, 80),
                s(916, 1688, 242, 161, 8, true, 80)
        ), bp(986, 273), bp(986, 273));

        add("GamePlayingLastSeconds", "GamePlaying", colors(
                s(916, 198, 181, 207, 74, true, 80),
                s(916, 318, 190, 174, 57, true, 80),
                s(916, 1688, 181, 178, 74, true, 80)
        ), bp(986, 273), bp(986, 273));

        add("GamePlaying", "GamePlaying", colors(
                s(916, 198, 230, 200, 20, true, 80),
                s(916, 318, 214, 191, 28, true, 80),
                s(916, 1688, 214, 191, 28, true, 80)
        ), bp(986, 273), bp(986, 273));

        add("GamePlaying2", "GamePlaying", colors(
                s(980, 258, 190, 244, 70, true, 80),
                s(852, 258, 244, 197, 20, true, 80),
                s(916, 1688, 238, 150, 25, true, 80)
        ), bp(986, 273), bp(986, 273));

        addOnDetect("RootDetectionLdp1080p480dpiEn", "RootDetectionLdp1080p480dpiEn", colors(
                s(80, 690, 255, 255, 255, true, 25),
                s(70, 680, 255, 255, 255, false, 25),
                s(1000, 1300, 255, 255, 255, true, 25),
                s(1010, 1310, 255, 255, 255, false, 25)
        ), bp(855, 1224), bp(855, 1224), SWITCH_TO_STARTUP_MODE);

        addOnDetect("RootDetectionLdp1080p480dpiJp", "RootDetectionLdp1080p480dpiJp", colors(
                s(80, 635, 255, 255, 255, true, 25),
                s(70, 625, 255, 255, 255, false, 25),
                s(1000, 1360, 255, 255, 255, true, 25),
                s(1010, 1370, 255, 255, 255, false, 25)
        ), bp(850, 1280), bp(850, 1280), SWITCH_TO_STARTUP_MODE);

        addOnDetect("RootDetectionLdp480x800x160dpiEn", "RootDetectionLdp480x800x160dpiEn", colors(
                s(90, 780, 253, 253, 253, true, 25),
                s(65, 745, 255, 255, 255, false, 25),
                s(990, 1190, 252, 252, 252, true, 25),
                s(1015, 1225, 255, 255, 255, false, 25)
        ), bp(885, 1135), bp(885, 1135), SWITCH_TO_STARTUP_MODE);

        addOnDetect("RootDetectionNox1080p360dpiEn", "RootDetectionNox1080p360dpiEn", colors(
                s(135, 795, 255, 255, 255, true, 25),
                s(125, 785, 255, 255, 255, false, 25),
                s(945, 1170, 255, 255, 255, true, 25),
                s(955, 1180, 255, 255, 255, false, 25)
        ), bp(850, 1115), bp(850, 1115), SWITCH_TO_STARTUP_MODE);

        addOnDetect("RootDetectionNox480x800x160dpiJp", "RootDetectionNox480x800x160dpiJp", colors(
                s(85, 735, 255, 255, 255, true, 25),
                s(75, 725, 255, 255, 255, false, 25),
                s(995, 1240, 255, 255, 255, true, 25),
                s(1005, 1250, 255, 255, 255, false, 25)
        ), bp(885, 1170), bp(885, 1170), SWITCH_TO_STARTUP_MODE);

        addOnDetect("RootDetectionNox480x800x160dpiEn", "RootDetectionNox480x800x160dpiEn", colors(
                s(85, 760, 255, 255, 255, true, 25),
                s(75, 750, 255, 255, 255, false, 25),
                s(995, 1215, 255, 255, 255, true, 25),
                s(1005, 1225, 255, 255, 255, false, 25)
        ), bp(885, 1150), bp(885, 1150), SWITCH_TO_STARTUP_MODE);

        addOnDetect("RootDetectionSamsungA20En", "RootDetectionSamsungA20En", colors(
                s(60, 440, 255, 255, 255, true, 25),
                s(50, 440, 255, 255, 255, false, 25),
                s(60, 430, 255, 255, 255, false, 25),
                s(1020, 1310, 255, 255, 255, true, 25),
                s(1020, 1320, 255, 255, 255, false, 25),
                s(1010, 1325, 255, 255, 255, false, 25)
        ), bp(850, 1230), bp(850, 1230), SWITCH_TO_STARTUP_MODE);

        add("MagicalTime", "MagicalTime", colors(
                s(817, 587, 244, 249, 243, true, 80),
                s(594, 857, 248, 182, 121, true, 100),
                s(208, 1217, 236, 175, 9, true, 80),
                s(662, 1213, 232, 171, 5, true, 80)
        ), bp(381, 1221), bp(856, 1221));

        add("OutOfMedals", "OutOfMedals", colors(
                s(127, 873, 74, 74, 74, true, 80),
                s(186, 898, 255, 213, 188, true, 80),
                s(865, 879, 247, 251, 255, true, 80),
                s(474, 1065, 238, 174, 8, true, 80),
                s(540, 1070, 56, 91, 140, true, 80),
                s(595, 1066, 238, 171, 8, true, 80)
        ), bp(300, 1080), bp(300, 1080));

        add("NetworkDisable", "NetworkDisable", colors(
                s(478, 1080, 236, 94, 116, true, 80),
                s(932, 1077, 232, 171, 5, true, 80)
        ), bp(885, 1080), bp(885, 1084));

        add("NetworkTimeout", "NetworkTimeout", colors(
                s(530, 590, 33, 197, 234, true, 80),
                s(530, 620, 59, 94, 148, true, 80),
                s(478, 1080, 232, 171, 5, true, 80),
                s(932, 1077, 232, 171, 5, true, 80),
                s(530, 1150, 59, 94, 148, true, 80),
                s(530, 1170, 33, 197, 234, true, 80)
        ), bp(885, 1084), bp(885, 1084));

        add("FriendInfo", "FriendInfo", colors(
                s(565, 576, 31, 190, 220, true, 80),
                s(547, 1195, 27, 192, 222, true, 80),
                s(554, 1332, 238, 186, 12, true, 80)
        ), bp(576, 1408), bp(576, 1408));

        add("LevelUp", "LevelUp", colors(
                s(140, 1656, 233, 175, 6, true, 80),
                s(450, 1656, 233, 175, 6, true, 80),
                s(620, 1656, 233, 175, 6, true, 80),
                s(930, 1656, 233, 175, 6, true, 80)
        ), bp(300, 1660), bp(300, 1660));

        add("HighScore", "HighScore", colors(
                s(576, 1325, 238, 187, 10, true, 80),
                s(576, 1082, 33, 194, 231, true, 80),
                s(576, 762, 33, 194, 231, true, 80),
                s(576, 820, 64, 189, 171, true, 80)
        ), bp(576, 1325), bp(576, 1325));

        add("ClosePage", "ClosePage", colors(
                s(540, 1588, 233, 180, 10, true, 80)
        ), bp(576, 1660), bp(576, 1660));

        add("ReceiveSkillTicket", "ReceiveSkillTicket", colors(
                s(405, 806, 240, 155, 20, true, 80),
                s(488, 839, 244, 164, 23, true, 80),
                s(502, 821, 255, 255, 255, true, 40),
                s(390, 824, 58, 92, 142, true, 80),
                s(522, 812, 60, 95, 147, true, 80),
                s(874, 1098, 238, 174, 8, true, 80),
                s(198, 1095, 239, 174, 8, true, 80),
                s(160, 1545, 0, 4, 8, true, 80),
                s(526, 553, 33, 195, 231, true, 80)
        ), bp(198, 1095), bp(874, 1098));

        add("ReceivePremiumTicket", "ReceivePremiumTicket", colors(
                s(405, 806, 216, 20, 25, true, 80),
                s(488, 839, 208, 20, 23, true, 80),
                s(502, 821, 255, 247, 181, true, 40),
                s(390, 824, 58, 92, 142, true, 80),
                s(522, 812, 60, 95, 147, true, 80),
                s(874, 1098, 238, 174, 8, true, 80),
                s(198, 1095, 239, 174, 8, true, 80),
                s(160, 1545, 0, 4, 8, true, 80),
                s(526, 553, 33, 195, 231, true, 80)
        ), bp(198, 1095), bp(874, 1098));

        add("ReceiveHeartWithoutCoins", "ReceiveHeartWithoutCoins", colors(
                s(360, 570, 33, 198, 233, true, 30),
                s(400, 620, 61, 94, 147, true, 30),
                s(460, 820, 222, 61, 148, true, 30),
                s(420, 1100, 238, 174, 8, true, 30),
                s(860, 1100, 238, 174, 8, true, 30),
                s(540, 1100, 58, 94, 146, true, 30),
                s(550, 1600, 49, 36, 0, true, 30)
        ), bp(420, 1100), bp(860, 1100));

        add("ExtraUpdateJp", "ExtraUpdate", colors(
                s(104, 556, 36, 204, 239, true, 80),
                s(104, 1194, 36, 204, 239, true, 80),
                s(700, 1100, 238, 174, 8, true, 80),
                s(200, 1100, 238, 174, 8, true, 80),
                s(644, 676, 248, 248, 248, true, 80),
                s(694, 676, 248, 248, 248, true, 80),
                s(668, 676, 58, 93, 148, true, 80),
                s(422, 998, 48, 93, 148, true, 80),
                s(406, 998, 248, 248, 248, true, 80),
                s(434, 998, 248, 248, 248, true, 80)
        ), bp(770, 1100), bp(770, 1100));

        add("ExtraUpdateEn", "ExtraUpdate", colors(
                s(104, 556, 36, 204, 239, true, 80),
                s(104, 1194, 36, 204, 239, true, 80),
                s(700, 1100, 238, 174, 8, true, 80),
                s(200, 1100, 238, 174, 8, true, 80),
                s(520, 680, 248, 248, 248, true, 80),
                s(558, 680, 248, 248, 248, true, 80),
                s(538, 680, 55, 94, 148, true, 80),
                s(674, 1002, 60, 100, 150, true, 80),
                s(662, 1002, 240, 240, 240, true, 80),
                s(686, 1002, 240, 240, 240, true, 80)
        ), bp(770, 1100), bp(770, 1100));

        add("RubyResetDifficulty", "RubyResetDifficulty", colors(
                s(594, 972, 247, 81, 82, true, 80),
                s(610, 1166, 189, 0, 41, true, 80),
                s(588, 1096, 25, 174, 214, true, 80),
                s(867, 1270, 238, 174, 8, true, 80),
                s(425, 1275, 238, 174, 8, true, 80)
        ), bp(425, 1275), bp(867, 1270));
    }

    /** All pages, in the same order as the original {@code Page} object (used by findPageObject's scan order). */
    public static List<PageDefinition> all() {
        return new java.util.ArrayList<>(PAGES.values());
    }

    /** Looks up a single page definition by its table key (e.g. "ClosePage", "FriendInfo"). */
    public static PageDefinition get(String key) {
        return PAGES.get(key);
    }
}