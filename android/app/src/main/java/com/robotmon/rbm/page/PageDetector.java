package com.robotmon.rbm.page;

import com.robotmon.rbm.capture.LatestFrameHolder;
import com.robotmon.rbm.config.PageCatalog;
import com.robotmon.rbm.geometry.GameCoordinateMapper;
import com.robotmon.rbm.model.ButtonPoint;
import com.robotmon.rbm.model.PageColorSample;
import com.robotmon.rbm.model.PageDefinition;
import com.robotmon.rbm.model.Point2D;
import com.robotmon.rbm.model.RgbColor;
import com.robotmon.rbm.util.ColorUtils;

import org.opencv.core.Mat;
import java.util.List;
/**
 * Recognizes which game screen is currently showing by sampling pixel colors
 * from the latest full-screen menu frame. Ported from
 * Tsum.prototype.findPageObject()/findPage()/matchesPage()/isOnScreenshot()
 * in index.js.
 */
public class PageDetector {
    /** Side-effect run when a RootDetection* page is first seen; sets isStartupPhase=true. */
    public static final String ON_DETECT_SWITCH_TO_STARTUP_MODE = "switchToStartupMode";

    private final LatestFrameHolder frameHolder;
    private final GameCoordinateMapper mapper;

    public PageDetector(LatestFrameHolder frameHolder, GameCoordinateMapper mapper) {
        this.frameHolder = frameHolder;
        this.mapper = mapper;
    }

    /** Reads the pixel color at logical coordinates (@code xy}, clamped to (0,0) like the original getColor(). */
    public RgbColor getColor(Mat img, double x, double y) {
        Point2D r = mapper.toResizeXY(x, y);
        int rx = (int) Math.max(r.x, 0);
        int ry = (int) Math.max(r.y, 0);
        rx = Math.min(rx, img.cols() - 1);
        ry = Math.min(ry, img.rows() - 1);
        double[] px = img.get(ry, rx);
        // img is BGR.
        return new RgbColor((int) px[2], (int) px[1], (int) px[0]);
    }

    /**
     * Repeatedly screenshots and scans the Page table until a page matches or
     * {@code timeoutMs} elapses. Ties: (diff &lt; threshold) === match for
     * every color sample must hold. Runs the page's onDetect action, if any.
     *
     * @param times snapshots to try per attempt loop (default 2)
     * @param timeoutMs give up after this many ms with no match (default 700)
     */
    public PageDefinition findPageObject(int times, long timeoutMs, Runnable onSwitchToStartupMode) {
        long start = System.currentTimeMillis();
        List<PageDefinition> pages = PageCatalog.all();
        while (true) {
            PageDefinition matched = null;
            for (int t = 0; t < times; t++) {
                Mat img = frameHolder.getClone();
                if (img == null) {
                    sleepQuiet(100);
                    continue;
                }
                matched = null;
                for (PageDefinition page : pages) {
                    boolean ok = true;
                    List<PageColorSample> colors = page.colors;
                    for (PageColorSample sample : colors) {
                        RgbColor actual = getColor(img, sample.x, sample.y);
                        int diff = ColorUtils.absColorDistance(sample.color, actual);
                        if ((diff < sample.threshold) != sample.match) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok && !colors.isEmpty()) {
                        matched = page;
                        break;
                    }
                }
                img.release();
                sleepQuiet(100);
                if (matched != null) {
                    break;
                }
            }
            if (matched != null) {
                if (ON_DETECT_SWITCH_TO_STARTUP_MODE.equals(matched.onDetectAction) && onSwitchToStartupMode != null) {
                    onSwitchToStartupMode.run();
                }
                return matched;
            }
            if (System.currentTimeMillis() - start > timeoutMs) {
                return null;
            }
        }
    }

    public PageDefinition findPageObject() {
        return findPageObject(2, 700, null);
    }

    /**
     * Like {@link #findPageObject}, but returns the page name string
     * ("unknown" if none matched). Callers historically used this name to
     * detect entry into a handful of "stable" screens.
     */
    public String findPage(int times, long timeoutMs, Runnable onSwitchToStartupMode) {
        PageDefinition page = findPageObject(times, timeoutMs, onSwitchToStartupMode);
        return page != null ? page.name : "unknown";
    }

    public String findPage() {
        return findPage(2, 700, null);
    }

    /**
     * Looser check: does the CURRENT screen match {@code pageName} (any page
     * table entry sharing that display name), using isSameColor(..., 20)
     * instead of findPageObject's strict "(diff&lt;threshold)===match" rule.
     * Ported from Tsum.prototype.matchesPage().
     */
    public boolean matchesPage(String pageName) {
        Mat img = null;
        boolean found = false;
        try {
            for (PageDefinition page : PageCatalog.all()) {
                if (!pageName.equals(page.name)) {
                    continue;
                }
                if (img == null) {
                    img = frameHolder.getClone();
                    if (img == null) {
                        return false;
                    }
                }
                found = true;
                for (PageColorSample sample : page.colors) {
                    RgbColor actual = getColor(img, sample.x, sample.y);
                    if (!ColorUtils.isSameColor(actual, sample.color, 20)) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        } finally {
            if (img != null) {
                img.release();
            }
        }
        return found;
    }

    /**
     * Checks whether a single button-point's expected color is currently on
     * screen. Ported from Tsum.prototype.isOnScreenshot(). {@code colorDiff}
     * defaults to 20, matching isSameColor()'s default.
     */
    public boolean isOnScreenshot(Mat img, ButtonPoint point, int colorDiff) {
        if (point == null || point.color == null) {
            return false;
        }
        RgbColor actual = getColor(img, point.x, point.y);
        return ColorUtils.isSameColor(point.color, actual, colorDiff);
    }

    public boolean isOnScreenshot(Mat img, ButtonPoint point) {
        return isOnScreenshot(img, point, 20);
    }

    private void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}