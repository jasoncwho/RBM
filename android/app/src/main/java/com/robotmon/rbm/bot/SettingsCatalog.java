package com.robotmon.rbm.bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The UI schema for the settings screen: grouped {@link SettingDef}s, ported
 * from index.html's {@code settings} array (same grouping/order/titles/
 * ranges). Entries whose original setting has no backing implementation in
 * this port (jpVersion, specialScreenRatio, autobuyBoxes, maxChain override,
 * handleLongSkillAnimations, unlockLevelHoursWait, tsumMonitorUrl,
 * tsumAppRestartFrequency, skillAutoTap, recordsenderEnlarge, debug toggles,
 * pauseWhencalc -- see README "What was intentionally not ported") are left
 * out rather than rendered as controls that would silently do nothing.
 *
 * <p>Defaults are read off a fresh {@link BotSettings} instance so this file
 * and {@link BotSettings}'s field initializers can't drift apart.
 */
public final class SettingsCatalog {
    private SettingsCatalog() {}

    public static final String[] SKILL_TYPE_KEYS = {
        "burst", "burst_bubbles",
        "block_donald_s", "block_donaldx_s", "block_lukej_s", "block_moana_s",
        "block_marie_s", "block_missbunny_s", "block_rabbit_s", "block_mickeyh2015_s",
        "block_snowwhite_s", "block_cinderella_s", "block_woody2_s", "block_cabbage_mickey_s",
        "block_cpt_ly_s", "block_lightning_mcqueen_plus_s", "block_rapunzel_plus_s",
        "block_tiara_minnie_plus_s", "block_pair_tsum", "no_skill"
    };

    public static final String[] SKILL_TYPE_TITLES = {
        "Burst", "Burst bubbles",
        "Donald", "Holiday Donald", "Jedi Luke", "Moana",
        "Marie", "Miss Bunny", "Rabbit", "Horn Hat Mickey",
        "Snow White", "Cinderella", "Sheriff Woody", "Cabbage Mickey",
        "Cpt. Lightyear", "Lightning McQueen+", "Rapunzel+",
        "Tiara Minnie+", "Pair Tsum", "No Skill"
    };

    public static class Group {
        public final String heading;
        public final List<SettingDef> items;

        Group(String heading, List<SettingDef> items) {
            this.heading = heading;
            this.items = items;
        }
    }

    public static List<Group> groups() {
        BotSettings d = new BotSettings();
        List<Group> groups = new ArrayList<>();

        groups.add(new Group("General", Arrays.asList(
            SettingDef.bool("autoLaunchApp", "Auto Launch Tsum App (no root/shell access -- logs a warning, does nothing)", d.autoLaunchApp)
        )));

        groups.add(new Group("Play", Arrays.asList(
            SettingDef.bool("autoPlayGame", "Auto Play Game", d.autoPlayGame),
            SettingDef.bool("clearBubbles", "Clear Bubbles", d.clearBubbles),
            SettingDef.bool("useFan", "Use Fan?", d.useFan),
            SettingDef.bool("bonusScore", "+Score", d.bonusScore),
            SettingDef.bool("bonusCoin", "+Coin", d.bonusCoin),
            SettingDef.bool("bonusExp", "+Exp", d.bonusExp),
            SettingDef.bool("bonusTime", "+Time", d.bonusTime),
            SettingDef.bool("bonusBubble", "+Bubble", d.bonusBubble),
            SettingDef.bool("bonus5to4", "5>4", d.bonus5to4),
            SettingDef.bool("bonusCombo", "+Combo", d.bonusCombo),
            SettingDef.number("skillWaitingTime", "Skill Waiting time (sec)", d.skillWaitingTimeSec, 1, 15, 1),
            SettingDef.number("skillLevel", "Skill Level", d.skillLevel, 1, 6, 1),
            SettingDef.dropdown("skillType", "Skill Type", d.skillType, SKILL_TYPE_KEYS, SKILL_TYPE_TITLES),
            SettingDef.number("noSkillLastFeverSec", "No skill last fever seconds", d.noSkillLastFeverSec, 0, 10, 1)
        )));

        groups.add(new Group("Receive All Hearts", Arrays.asList(
            SettingDef.bool("receiveAllHearts", "Receive All Hearts", d.receiveAllHearts),
            SettingDef.number("receiveAllHeartsMinWait", "Waiting time (min) before repeat", d.receiveAllHeartsMinWaitMinutes, 5, 60, 5)
        )));

        groups.add(new Group("Receive Hearts One By One", Arrays.asList(
            SettingDef.bool("receiveHeartsOneByOne", "Receive Hearts One By One", d.receiveHeartsOneByOne),
            SettingDef.bool("receiveHeartsSkipFirst", "Skip first person", d.receiveHeartsSkipFirst),
            SettingDef.bool("receiveHeartsSkipRuby", "Skip Ruby", d.keepRuby),
            SettingDef.bool("claimAllWithoutCoins", "Claim All old mails", d.claimAllWithoutCoins),
            SettingDef.number("mailOpenMax", "Max Times to Open Mailbox", d.receiveCheckLimit, 1, 20, 1),
            SettingDef.number("mailMinWait", "Waiting time (min) before repeat", d.mailMinWaitMinutes, 1, 60, 2),
            SettingDef.bool("recordSender", "Record Sender", d.recordSender)
        )));

        groups.add(new Group("Send Hearts", Arrays.asList(
            SettingDef.bool("sendHeartsAuto", "Auto Send Hearts", d.sendHeartsAuto),
            SettingDef.bool("sendHeartsToZeroScore", "Send to 0 score", d.sendHeartsToZeroScore),
            SettingDef.number("sendHeartsMaxRuntime", "Max run time (min) [Start from the first place if time is 0]", d.sendHeartsMaxRuntimeMinutes, 0, 80, 5),
            SettingDef.number("sendHeartsMinWait", "Waiting time (min) before repeat", d.sendHeartsMinWaitMinutes, 1, 60, 5)
        )));

        return groups;
    }
}