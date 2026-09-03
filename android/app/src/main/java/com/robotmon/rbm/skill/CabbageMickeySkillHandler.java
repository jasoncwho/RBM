package com.robotmon.rbm.skill;

import com.robotmon.rbm.bot.BotContext;
import com.robotmon.rbm.model.RgbColor;
import com.robotmon.rbm.util.ColorUtils;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Cabbage Mickey's skill: hunts the full screen for Mickey's face color
 * hidden among the cabbages and taps it repeatedly, falling back to clearing
 * the bubble grid if he isn't found. Ported from the block_cabbage_mickey_s
 * branch of Tsum.prototype.useSkill().
 *
 * <p>The original blurs the screenshot with a custom smooth() before
 * sampling; here that is approximated with a plain box blur, which is close
 * enough for the coarse per-cell threshold check below.
 */
public class CabbageMickeySkillHandler implements SkillHandler {
    private static final RgbColor MICKEY_FACE_COLOR = new RgbColor(245, 225, 210);
    private static final int MAX_TRIES = 5;

    @Override
    public boolean run(SkillController sc, BotContext ctx) {
        ctx.sleep(3300); // wait for all cabbages to be placed

        double[] found = findMickey(sc, ctx);
        if (found != null) {
            for (int i = 0; i < 10; i++) {
                sc.tapLogical(found[0] + 15, found[1] + 15, 50);
            }
            ctx.sleep(1000);
        } else {
            sc.clearAllBubbles();
        }
        return true;
    }

    /** @return {x, y} of Mickey's face if found, else null. */
    private double[] findMickey(SkillController sc, BotContext ctx) {
        for (int tries = 1; tries <= MAX_TRIES; tries++) {
            ctx.sleep(100);
            Mat img = ctx.frameHolder.getClone();
            if (img == null) {
                continue;
            }
            try {
                Imgproc.blur(img, img, new Size(5, 5));
                for (double y = 720; y < 1380; y += 25) {
                    for (double x = 120; x < 1000; x += 60) {
                        RgbColor color = ctx.pageDetector.getColor(img, x, y);
                        if (!ColorUtils.isSameColor(MICKEY_FACE_COLOR, color, 20)) {
                            continue;
                        }
                       
                        RgbColor up = ctx.pageDetector.getColor(img, x, y - 10);
                        RgbColor down = ctx.pageDetector.getColor(img, x, y + 10);
                        RgbColor left = ctx.pageDetector.getColor(img, x - 10, y);
                        RgbColor right = ctx.pageDetector.getColor(img, x + 10, y);
                        boolean confirmed =
                            (ColorUtils.isSameColor(MICKEY_FACE_COLOR, up, 20)
                            || ColorUtils.isSameColor(MICKEY_FACE_COLOR, down, 20))
                            && (ColorUtils.isSameColor(MICKEY_FACE_COLOR, left, 20)
                            || ColorUtils.isSameColor(MICKEY_FACE_COLOR, right, 20));
                        if (confirmed) {
                            return new double[]{x, y};
                        }
                    }
                }
            } finally {
                img.release();
            }
        }
        return null;
    }
}
