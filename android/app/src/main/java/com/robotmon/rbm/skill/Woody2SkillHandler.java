package com.robotmon.rbm.skill;

import com.robotmon.rbm.bot.BotContext;
import com.robotmon.rbm.input.InputController;

/**
 * Woody 2's skill: one long left-right sweep. Ported from the block_woody2_s
 * branch of Tsum.prototype.useSkill().
 */
public class Woody2SkillHandler implements SkillHandler {
    @Override
    public boolean run(SkillController sc, BotContext ctx) {
        ctx.sleep(1800);
        InputController.DragSession drag = ctx.input.beginDrag(ctx.mapper.toRealXY(540, 960), 20);
        drag.moveTo(ctx.mapper.toRealXY(980, 960), 20);
        ctx.sleep(50);
        for (int i = 0; i < 3; i++) {
            drag.moveTo(ctx.mapper.toRealXY(100, 960), 20);
            ctx.sleep(420);
            drag.moveTo(ctx.mapper.toRealXY(980, 960), 20);
            ctx.sleep(480);
        }
        drag.up(20);
        return true;
    }
}