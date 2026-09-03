package com.robotmon.rbm.skill;

import com.robotmon.rbm.bot.BotContext;
import com.robotmon.rbm.model.RgbColor;
import com.robotmon.rbm.util.ColorUtils;

import org.opencv.core.Mat;

/**
 * Lightning McQueen+'s skill: waits until the board-area capture shows the
 * "max speed" red pixel, then taps once. Ported from the
 * block_lightning_mcqueen_plus_s branch of Tsum.prototype.useSkill().
 */
public class LightningMcQueenPlusSkillHandler implements SkillHandler {
    private static final RgbColor MAX_SPEED_COLOR = new RgbColor(245, 0, 0);

    @Override
    public boolean run(SkillController sc, BotContext ctx) {
        ctx.sleep(2000);
        for (int i = 1; i <= 20; i++) {
            ctx.sleep(50);
            Mat img = sc.boardFrame();
            if (img == null) {
                continue;
            }
            RgbColor color;
            try {
                color = rawBgrPixel(img, 120, 184);
            } finally {
                img.release();
            }
            if (ColorUtils.isSameColor(MAX_SPEED_COLOR, color, 10)) {
                sc.tapLogical(670, 1050, 10); // tap somewhere into the game
                break;
            }
        }
        ctx.sleep(2500);
        return true;
    }

    /** Reads a BGR pixel at raw capture coordinates (no resize-space scaling), matching getImageColor(). */
    private RgbColor rawBgrPixel(Mat bgr, int x, int y) {
        int cx = Math.max(0, Math.min(bgr.cols() - 1, x));
        int cy = Math.max(0, Math.min(bgr.rows() - 1, y));
        double[] px = bgr.get(cy, cx);
        return new RgbColor((int) px[2], (int) px[1], (int) px[0]);
    }
}
