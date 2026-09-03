package com.robotmon.rbm.skill;

import com.robotmon.rbm.bot.BotContext;
import com.robotmon.rbm.config.ButtonCatalog;
import com.robotmon.rbm.model.Bubble;

import org.opencv.core.Mat;

import java.util.List;

/**
 * Captain Li Shang's skill: several taps keyed on skill level, then popping
 * any bonus bubbles the resulting board scan finds. Ported from the
 * block_cpt_ly_s branch of Tsum.prototype.useSkill().
 */
public class CptLySkillHandler implements SkillHandler {
    @Override
    public boolean run(SkillController sc, BotContext ctx) {
        sc.tapLogical(ButtonCatalog.GAME_RAND, 100);
        ctx.sleep(2100);
        sc.tapLogical(ButtonCatalog.SKILL_CPT_LY_1, 10);
        ctx.sleep(50);
        sc.tapLogical(ButtonCatalog.SKILL_CPT_LY_2, 10);
        if (ctx.skillLevel >= 2) {
            ctx.sleep(500);
            sc.tapLogical(ButtonCatalog.SKILL_CPT_LY_3, 10);
        }
        if (ctx.skillLevel >= 4) {
            ctx.sleep(500);
            sc.tapLogical(ButtonCatalog.SKILL_CPT_LY_3, 10);
        }
        if (ctx.skillLevel == 6) {
            ctx.sleep(550);
            sc.tapLogical(ButtonCatalog.SKILL_CPT_LY_3, 10);
        }

        Mat bubbleImg = sc.boardFrame();
        if (bubbleImg != null) {
            try {
                List<Bubble> bubbles = sc.getBubbleDetector().detect(bubbleImg);
                sc.setGameBubbles(bubbles);
            } finally {
                bubbleImg.release();
            }
        }
        sc.popGameBubbles();
        return true;
    }
}