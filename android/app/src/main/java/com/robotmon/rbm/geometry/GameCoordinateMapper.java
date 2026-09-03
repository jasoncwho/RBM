package com.robotmon.rbm.geometry;

import com.robotmon.rbm.model.Point2D;

/**
 * Converts between logical 1080x1920-reference coordinates (the space every
 * Page/Button table entry is defined in) and both a) real screen pixels
 * (for tapping) and b) the reduced-size screenshot used for reading colors.
 *
 * Ported from Tsum.prototype.init()/toResizeXY()/toRealXY() in index.js.
 */
public class GameCoordinateMapper {

    /** Resolution the full-screen menu screenshot is resized to before reading colors. */
    public static final int MENU_REFERENCE_WIDTH = 1080;

    private int screenWidth;
    private int screenHeight;
    private double gameOffsetX;
    private double gameOffsetY;
    private double gamewidth;
    private double gameHeight;
    private double captureGameRatio;
    private double resizeRatio;
    private int playOffsetX;
    private int playOffsetY;
    private int playWidth;
    private int playHeight;

    /**
     * @param screenWidth real device screen width, px
     * @param screenHeight real device screen height, px
     */
    public void init(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        boolean isFat = (double) screenHeight / screenWidth < 1.5;
        if (isFat) {
            gameHeight = screenHeight;
            gamewidth = screenHeight / 1.5;
            gameOffsetY = Math.floor((gamewidth * 16 / 9 - gameHeight) / 2);
            gameOffsetX = Math.floor((gamewidth - screenWidth) / 2);
        } else {
            gamewidth = screenWidth;
            gameHeight = screenWidth / 9.0 * 16;
            gameOffsetX = 0;
            gameOffsetY = Math.floor((gameHeight - screenHeight) / 2);
        }

        captureGameRatio = gamewidth / 1080.0;
        if (isFat) {
            playWidth = (int) gamewidth;
            playOffsetX = (int) Math.max(-gameOffsetX, 0);
        } else {
            playWidth = screenWidth;
            playOffsetX = 0;
        }
        playHeight = playWidth;
        playOffsetY = (int) Math.round(465 * captureGameRatio - gameOffsetY);

        // Normalizes page screenshots to a 360px-wide reference, same as the original.
        resizeRatio = Math.max(1, screenWidth / 360.0);

        // NOTE: the original also had a "special screen ratio" branch (detect &&
        // screenHeight/screenWidth > 16/9) that re-measured gameOffsetY by
        // scanning a live screenshot for the game's top/bottom borders
        // ((detectOffsetYinGame())). That correction is not ported here -- it
        // requires a screenshot during setup -- so very long/notched screens may
        // need gameOffsetY nudged manually; see the README.
    }

    /** Maps logical coordinates to the reduced-size screenshot used for color reads. */
    public Point2D toResizeXY(double x, double y) {
        double rx = Math.floor((x * captureGameRatio - gameOffsetX) / resizeRatio);
        double ry = Math.floor((y * captureGameRatio - gameOffsetY) / resizeRatio);
        return new Point2D(rx, ry);
    }

    /** Maps logical coordinates to real screen pixels, for tapping. */
    public Point2D toRealXY(double x, double y) {
        double rx = Math.floor(x * captureGameRatio - gameOffsetX);
        double ry = Math.floor(y * captureGameRatio - gameOffsetY);
        return new Point2D(rx, ry);
    }

    public double getResizeRatio() {
        return resizeRatio;
    }

    public double getResizerRatio() {
        return resizeRatio;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getPlayOffsetX() {
        return playOffsetX;
    }

    public int getPlayOffsetY() {
        return playOffsetY;
    }

    public int getPlayWidth() {
        return playWidth;
    }

    public int getPlayHeight() {
        return playHeight;
    }
}