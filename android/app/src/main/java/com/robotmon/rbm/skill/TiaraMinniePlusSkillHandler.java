package com.robotmon.rbm.skill;

import com.robotmon.rbm.bot.BotContext;
import com.robotmon.rbm.model.RgbColor;

import org.opencv.core.Mat;

/**
* Tiara Minnie+'s skill: blows up a present the player picks from three shown
* after a short "dream" animation.
*
* <p>Ported faithfully: {@link #waitForSettledBoard} (holding off activation
* until the board stops moving, via a brightness-fingerprint diff -- see
* tiaraBoardSignature()/tiaraWaitForSettledBoard() in index.js).
*
* <p><b>Not ported:</b> the original's present-recognition subsystem (a
* thought-bubble template captured live, matched by grid/hue against the
* three presents shown, described by TiaraMinnieConfig/tiaraCapture()/
* tiaraCompileTemplate() and the scoring loop in index.js) is a large,
* bespoke piece of image analysis that has not been reimplemented here. This
* handler waits out the same lead times the original does and then returns
* without tapping a present -- so Tiara Minnie+'s skill will activate but no
* automatic pick is made; a human (or a future contribution) needs to tap the
* present. This always returns {@code false}, matching the original (see the
* comment on that branch in index.js: an immediate re-fire is never correct
* for a skill whose choreography takes several seconds).
*/
public class TiaraMinniePlusSkillHandler implements SkillHandler {
   private static final long SETTLE_WAIT_MS = 320;
   private static final long SETTLE_MIN_MS = 60;
   private static final long SETTLE_POLL_MS = 30;
   private static final int SETTLE_GRID = 16;
   private static final double SETTLE_MAX_DIFF = 0.03;
   private static final int SETTLE_QUIET_SCANS = 2;

   private static final long DREAM_LEAD_MS = 1500;
   private static final long DREAM_WAIT_MS = 3000;

   @Override
   public boolean run(SkillController sc, BotContext ctx) {
       // waitForSettledBoard() already ran before the skill button tap (see
       // useSkill()); just wait out the dream/present lead here since no
       // present picking is performed.
       ctx.sleep(DREAM_LEAD_MS);
       ctx.sleep(DREAM_WAIT_MS);
       return false;
   }

   /** Ported from Tsum.prototype.tiaraWaitForSettledBoard()/tiaraBoardSignature(). */
   static void waitForSettledBoard(SkillController sc, BotContext ctx) {
       long start = System.currentTimeMillis();
       long deadline = start + SETTLE_WAIT_MS;
       double[] prev = boardSignature(sc);
       int quiet = 0;
       while (System.currentTimeMillis() < deadline) {
           ctx.sleep(SETTLE_POLL_MS);
           double[] now = boardSignature(sc);
           double diff = signatureDiff(prev, now);
           prev = now;
           quiet = diff <= SETTLE_MAX_DIFF ? quiet + 1 : 0;
           if (quiet >= SETTLE_QUIET_SCANS && System.currentTimeMillis() - start >= SETTLE_MIN_MS) {
               return;
           }
       }
       // Fire anyway: a late skill is worth more than a skipped one.
   }

   /** A coarse brightness fingerprint of the board, cheap enough to poll repeatedly. */
   private static double[] boardSignature(SkillController sc) {
       Mat img = sc.boardFrame();
       double[] out = new double[SETTLE_GRID * SETTLE_GRID];
       if (img == null) {
           return out;
       }
       try {
           double stepX = img.cols() / (double) SETTLE_GRID;
           double stepY = img.rows() / (double) SETTLE_GRID;
           int i = 0;
           for (int gy = 0; gy < SETTLE_GRID; gy++) {
               for (int gx = 0; gx < SETTLE_GRID; gx++) {
                   int px = Math.min(img.cols() - 1, (int) Math.floor((gx + 0.5) * stepX));
                   int py = Math.min(img.rows() - 1, (int) Math.floor((gy + 0.5) * stepY));
                   double[] bgr = img.get(py, px);
                   RgbColor c = new RgbColor((int) bgr[2], (int) bgr[1], (int) bgr[0]);
                   out[i++] = (c.r + c.g + c.b) / 3.0;
               }
           }
       } finally {
           img.release();
       }
       return out;
   }

   private static double signatureDiff(double[] a, double[] b) {
       double d = 0;
       for (int i = 0; i < a.length; i++) {
           d += Math.abs(a[i] - b[i]);
       }
       return d / a.length / 255.0;
   }
}