package com.robotmon.rbm.skill;

import com.robotmon.rbm.bot.BotContext;

/**
 * Rapunzel+'s skill: a single long zig-zag drag over the board. Ported from
 * Tsum.prototype.useRapunzelPlusSkill()/the block_rapunzel_plus_s branch of
 * useSkill().
 */
public class RapunzelPlusSkillHandler implements SkillHandler {
    @Override
    public boolean run(SkillController sc, BotContext ctx) {
        ctx.sleep(1000);
        java.util.List<com.robotmon.rbm.model.BoardPiece> path = buildPath();
        sc.dragLogicalBoardPoints(path, 25);
        return true;
    }

    private java.util.List<com.robotmon.rbm.model.BoardPiece> buildPath() {
        java.util.List<com.robotmon.rbm.model.BoardPiece> path = new java.util.ArrayList<>();
        addVertical(path, 30, false);
        addHorizontal(path, 60, 30, 70);
        addVertical(path, 70, true);
        addHorizontal(path, 170, 70, 110);
        addVertical(path, 110, false);
        addHorizontal(path, 60, 110, 150);
        addVertical(path, 150, true);
        return path;
    }

    private void addVertical(java.util.List<com.robotmon.rbm.model.BoardPiece> path, double x, boolean reverse) {
        if (!reverse) {
            for (double y = 170; y >= 60; y -= 10) {
                path.add(new com.robotmon.rbm.model.BoardPiece(0, x, y));
            }
        } else {
            for (double y = 60; y <= 170; y += 10) {
                path.add(new com.robotmon.rbm.model.BoardPiece(0, x, y));
            }
        }
    }

    private void addHorizontal(java.util.List<com.robotmon.rbm.model.BoardPiece> path, double y, double x1, double x2) {
        if (x1 < x2) {
            for (double x = x1; x <= x2; x += 10) {
                path.add(new com.robotmon.rbm.model.BoardPiece(0, x, y));
            }
        } else {
            for (double x = x1; x >= x2; x -= 10) {
                path.add(new com.robotmon.rbm.model.BoardPiece(0, x, y));
            }
        }
    }
}