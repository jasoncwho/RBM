package com.robotmon.rbm.bot;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists settings-screen values and turns them into a {@link BotSettings},
 * the Java equivalent of index.html's {@code loadSettings()}/{@code
 * saveSettings()}/{@code resetSettings()} (which round-tripped the same
 * key-value pairs through {@code localStorage.tsumtsumsettings2}).
 */
public class BotSettingsStore {
    private static final String PREFS_NAME = "tsumtsumsettings2";

    private final SharedPreferences prefs;

    public BotSettingsStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    public int getNumber(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    public String getDropdown(String key, String defaultvalue) {
        return prefs.getString(key, defaultvalue);
    }

    public void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public void putNumber(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    public void putDropdown(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    /** Clears every stored setting, reverting the next {@link #load()} to defaults. Ported from resetSettings(). */
    public void reset() {
        prefs.edit().clear().apply();
    }

    /** Builds a BotSettings from the currently stored values (falling back to defaults for anything unset). */
    public BotSettings load() {
        BotSettings s = new BotSettings();
        s.autoLaunchApp = getBoolean("autoLaunchApp", s.autoLaunchApp);

        s.autoPlayGame = getBoolean("autoPlayGame", s.autoPlayGame);
        s.clearBubbles = getBoolean("clearBubbles", s.clearBubbles);
        s.useFan = getBoolean("useFan", s.useFan);
        s.bonusScore = getBoolean("bonusScore", s.bonusScore);
        s.bonusCoin = getBoolean("bonusCoin", s.bonusCoin);
        s.bonusExp = getBoolean("bonusExp", s.bonusExp);
        s.bonusTime = getBoolean("bonusTime", s.bonusTime);
        s.bonusBubble = getBoolean("bonusBubble", s.bonusBubble);
        s.bonus5to4 = getBoolean("bonus5to4", s.bonus5to4);
        s.bonusCombo = getBoolean("bonusCombo", s.bonusCombo);
        s.skillWaitingTimeSec = getNumber("skillWaitingTime", s.skillWaitingTimeSec);
        s.skillLevel = getNumber("skillLevel", s.skillLevel);
        s.skillType = getDropdown("skillType", s.skillType);
        s.noSkillLastFeverSec = getNumber("noSkillLastFeverSec", s.noSkillLastFeverSec);

        s.receiveAllHearts = getBoolean("receiveAllHearts", s.receiveAllHearts);
        s.receiveAllHeartsMinWaitMinutes = getNumber("receiveAllHeartsMinWait", s.receiveAllHeartsMinWaitMinutes);

        s.receiveHeartsOneByOne = getBoolean("receiveHeartsOneByOne", s.receiveHeartsOneByOne);
        s.receiveHeartsSkipFirst = getBoolean("receiveHeartsSkipFirst", s.receiveHeartsSkipFirst);
        s.keepRuby = getBoolean("receiveHeartsSkipRuby", s.keepRuby);
        s.claimAllWithoutCoins = getBoolean("claimAllWithoutCoins", s.claimAllWithoutCoins);
        s.receiveCheckLimit = getNumber("mailOpenMax", s.receiveCheckLimit);
        s.mailMinWaitMinutes = getNumber("mailMinWait", s.mailMinWaitMinutes);
        s.recordSender = getBoolean("recordSender", s.recordSender);

        s.sendHeartsAuto = getBoolean("sendHeartsAuto", s.sendHeartsAuto);
        s.sendHeartsToZeroScore = getBoolean("sendHeartsToZeroScore", s.sendHeartsToZeroScore);
        s.sendHeartsMaxRuntimeMinutes = getNumber("sendHeartsMaxRuntime", s.sendHeartsMaxRuntimeMinutes);
        s.sendHeartsMinWaitMinutes = getNumber("sendHeartsMinWait", s.sendHeartsMinWaitMinutes);

        return s;
    }
}