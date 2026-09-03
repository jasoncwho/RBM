package com.robotmon.rbm.skill;

import com.robotmon.rbm.bot.BotContext;

/**
 * Generic "clear the bonus-bubble grid once" skill (Marie, Miss Bunny,
 * Rabbit, Moana, Mickey (2015)). Ported from their identical
 * {@code this.clearAllBubbles(startDelay, endDelay)} branches in
 * Tsum.prototype.useSkill() -- they only differ in the delay before tapping.
 */
public class ClearBubblesSkillHandler implements SkillHandler {
    private final Long startDelay;
    private final Long endDelay;
    private final Double fromY;
    private final long delayBetweenLines;

    public ClearBubblesSkillHandler(Long startDelay, Long endDelay, Double fromY, long delayBetweenLines) {
        this.startDelay = startDelay;
        this.endDelay = endDelay;
        this.fromY = fromY;
        this.delayBetweenLines = delayBetweenLines;
    }

    @Override
    public boolean run(SkillController sc, BotContext ctx) {
        sc.clearAllBubbles(startDelay, endDelay, fromY, delayBetweenLines);
        return true;
    }
}