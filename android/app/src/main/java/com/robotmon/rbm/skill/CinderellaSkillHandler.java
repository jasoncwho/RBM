package com.robotmon.rbm.skill;

import com.robotmon.rbm.bot.BotContext;
import com.robotmon.rbm.config.ButtonCatalog;
import com.robotmon.rbm.model.BoardPiece;

import java.util.ArrayList;
import java.util.List;

/**
 * Cinderella's skill: five repeated zig-zag drags across each half of the
 * board. Ported from Tsum.prototype.useCinderellaSkill()/the block_cinderella_s
 * branch of useSkill().
 */
public class CinderellaSkillHandler implements SkillHandler {
    @Override
    public boolean run(SkillController sc, BotContext ctx) {
        ctx.sleep(1500);
        for (int i = 0; i < 5; i++) {
            for (int offset = 0; offset <= 200; offset += 200) {
                List<BoardPiece> path = new ArrayList<>();
                for (double y = 170; y >= 70; y -= 20) {
                    path.add(new BoardPiece(0, Math.abs(offset - 10), y));
                }
                for (double y = 60; y <= 180; y += 20) {
                    path.add(new BoardPiece(0, Math.abs(offset - 40), y));
                }
                for (double y = 180; y >= 60; y -= 20) {
                    path.add(new BoardPiece(0, Math.abs(offset - 70), y));
                }
                for (double y = 60; y <= 180; y += 20) {
                    path.add(new BoardPiece(0, Math.abs(offset - 100), y));
                }
                sc.dragLogicalBoardPoints(path, 12);
            }
        }
        ctx.sleep(3000);
        double fromY = (ButtonCatalog.GAME_BUBBLES_FROM.y + ButtonCatalog.GAME_BUBBLES_TO.y) / 2.0;
        sc.clearAllBubbles(10L, 50L, fromY, 200L);
        return true;
    }
}
