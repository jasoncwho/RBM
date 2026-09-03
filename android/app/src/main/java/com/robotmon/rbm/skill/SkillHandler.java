package com.robotmon.rbm.skill;

import com.robotmon.rbm.bot.BotContext;

/**
 * One character's skill-activation choreography: whatever taps/drags happen
 * after the skill button has been tapped. Ported from the per-skillType
 * branches of Tsum.prototype.useSkill() in index.js.
 */
public interface SkillHandler {
    /**
     * @return whether the skill fired and should be treated as "used" --
     * matches useSkill()'s return value. Only Tiara Minnie+ intentionally
     * returns false (see TiaraMinniePlusSkillHandler for why).
     */
    boolean run(SkillController controller, BotContext ctx);
}
