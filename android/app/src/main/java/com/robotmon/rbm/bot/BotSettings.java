package com.robotmon.rbm.bot;

/**
 * User-configurable knobs, ported from the {@code settings} object passed
 * into the original's top-level {@code start(settings)} -- defaults here
 * match index.html's {@code settings} table exactly. See {@link SettingsCatalog}
 * for the UI schema (grouping/titles/ranges) and {@link BotSettingsStore}
 * for the persisted-preferences <-> BotSettings mapping (the Java equivalent of
 * index.html's loadSettings()/saveSettings() localStorage round-trip).
 */
public class BotSettings {

    // --- task enable/disable + scheduling (minutes between runs) ---
    public boolean autoPlayGame = true;

    public boolean receiveHeartsOneByOne = false;
    public int mailMinWaitMinutes = 5;

    public boolean receiveAllHearts = true;
    public int receiveAllHeartsMinWaitMinutes = 25;

    public boolean sendHeartsAuto = false;
    public int sendHeartsMinWaitMinutes = 26;
    /** Max minutes a single sendHearts run may take; 0 = unlimited. */
    public int sendHeartsMaxRuntimeMinutes = 0;
    public boolean sendHeartsToZeroScore = false;

    // --- mailbox / heart receiving ---
    public boolean keepRuby = false;
    public boolean receiveHeartsSkipFirst = false;
    public boolean recordSender = false;
    public int receiveCheckLimit = 5;
    public boolean claimAllWithoutCoins = false;

    /** No shell/root access to foreground-check or launch the game app -- stays a documented no-op (see README). */
    public boolean autoLaunchApp = false;

    // --- skill / board play ---
    public String skillType = "burst";
    public int skillLevel = 3;
    public int skillWaitingTimeSec = 3;
    public int noSkillLastFeverSec = 0;
    public boolean useFan = false;
    public boolean clearBubbles = false;

    // --- post-game bonus item toggles ---
    public boolean bonus5to4 = false;
    public boolean bonusScore = false;
    public boolean bonusCoin = false;
    public boolean bonusExp = false;
    public boolean bonusTime = false;
    public boolean bonusBubble = false;
    public boolean bonusCombo = false;

    public static long minutesToMs(int minutes) {
        return minutes * 60_000L;
    }
}