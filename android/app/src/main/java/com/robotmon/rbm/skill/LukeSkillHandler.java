package com.robotmon.rbm.skill;

import com.robotmon.rbm.bot.BotContext;
import com.robotmon.rbm.config.ButtonCatalog;
import com.robotmon.rbm.input.InputController;
import com.robotmon.rbm.model.Point2D;

/**
 * Luke's skill: several swipes up the screen, retapping the skill buttons in
 * between. Ported from the block_lukejs branch of Tsum.prototype.useskill().
 */
public class LukeSkillHandler implements SkillHandler {
    @Override
    public boolean run(SkillController sc, BotContext ctx) {
        InputController.DragSession drag = null;
        for (int i = 0; i < 5; i++) {
            drag = ctx.input.beginDrag(ctx.mapper.toRealXY(820, 1200), 20);
            drag.moveTo(ctx.mapper.toRealXY(820, 1150), 20);
            if (i == 0) {
                ctx.sleep(1160);
            }
        }
        ctx.sleep(350);
        drag.moveTo(ctx.mapper.toRealXY(825, 1000), 20);
        ctx.sleep(100);
        drag.moveTo(ctx.mapper.toRealXY(835, 800), 20);
        ctx.sleep(100);
        drag.moveTo(ctx.mapper.toRealXY(845, 600), 20);
        ctx.sleep(100);
        drag.moveTo(ctx.mapper.toRealXY(850, 450), 20);
        drag.up(20);
        ctx.sleep(20);
                ctx.sleep(400);
                sc.tapLogical(ButtonCatalog.SKILL_LUKE_1, 30);
                sc.tapLogical(ButtonCatalog.SKILL_LUKE_2, 30);
                sc.tapLogical(ButtonCatalog.SKILL_LUKE_3, 30);
                sc.tapLogical(ButtonCatalog.SKILL_LUKE_4, 30);
                ctx.sleep(400);
                return true;
        }
}