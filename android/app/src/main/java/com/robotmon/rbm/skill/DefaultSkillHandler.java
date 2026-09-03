package com.robotmon.rbm.skill;

import com.robotmon.rbm.bot.BotContext;
import com.robotmon.rbm.config.ButtonCatalog;

/**
 * Fallback for skillType == "default"/"burst"/"burst_bubbles" (and any
 * unrecognized skillType): randomizes tsums, waits out the skill interval,
 * and for "burst_bubbles" also clears the bubble grid once. Ported from the
 * trailing else branch of Tsum.prototype.useSkill().
 */
public class DefaultSkillHandler implements SkillHandler {
    @Override
    public boolean run(SkillController sc, BotContext ctx) {
        sc.tapLogical(ButtonCatalog.GAME_RAND, 100);
        ctx.sleep(ctx.skillInterval - 100);
        if ("burst_bubbles".equals(ctx.skillType)) {
            sc.clearAllBubbles(0L, 0L, 1000.0, 300L);
        }
        return true;
    }
}
