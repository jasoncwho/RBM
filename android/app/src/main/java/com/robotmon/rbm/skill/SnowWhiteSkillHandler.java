package com.robotmon.rbm.skill;

import com.robotmon.rbm.bot.BotContext;
import com.robotmon.rbm.config.ButtonCatalog;

/**
 * Snow White's skill: clears bubbles twice, first a full pass then a
 * centered one. Ported from the block_snowwhite_s branch of
 * Tsum.prototype.useSkill().
 */
public class SnowWhiteSkillHandler implements SkillHandler {
    @Override
    public boolean run(SkillController sc, BotContext ctx) {
        sc.clearAllBubbles(1300L, null, null, 0L);
        double fromY = (ButtonCatalog.GAME_BUBBLES_FROM.y + ButtonCatalog.GAME_BUBBLES_TO.y) / 2.0;
        sc.clearAllBubbles(10L, 50L, fromY, 0L);
        return true;
    }
}