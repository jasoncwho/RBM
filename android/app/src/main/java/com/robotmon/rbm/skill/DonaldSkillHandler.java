package com.robotmon.rbm.skill;

import com.robotmon.rbm.bot.BotContext;
import com.robotmon.rbm.config.ButtonCatalog;

/**
 * Donald / Donald X's skill: taps a 3x-repeated grid across the board.
 * Ported from the block_donald_s / block_donaldx_s branch of
 * Tsum.prototype.useSkill().
 */
public class DonaldSkillHandler implements SkillHandler {
    @Override
    public boolean run(SkillController sc, BotContext ctx) {
        for (int i = 0; i < 3; i++) {
            for (double bx = ButtonCatalog.GAME_BUBBLES_FROM.x - 40; bx <= ButtonCatalog.GAME_BUBBLES_TO.x + 40; bx += 158) {
                for (double by = ButtonCatalog.GAME_BUBBLES_FROM.y; by <= ButtonCatalog.GAME_BUBBLES_TO.y + 100; by += 158) {
                    sc.tapLogical(bx, by, 10);
                }
            }
        }
        return true;
    }
}