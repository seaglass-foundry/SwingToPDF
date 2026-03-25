package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.io.IOException;

import javax.swing.JScrollBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * Renders a {@link JScrollBar} as PDF vector graphics.
 *
 * <p>Draws a track background, two small chevron arrow buttons at the ends, and a
 * proportional thumb showing the current scroll position.  Works for both
 * vertical and horizontal orientations.
 *
 * <p>Registered before the generic container catch-all so that the L&F's
 * {@code BasicArrowButton} children are never visited individually.
 */
final class JScrollBarHandler implements ComponentHandler {

    static final JScrollBarHandler INSTANCE = new JScrollBarHandler();

    private JScrollBarHandler() {}

    /** Size of each end-cap arrow button in the scroll direction (pixels). */
    private static final int ARROW_SIZE = arrowSize();

    private static int arrowSize() {
        int w = UIManager.getInt("ScrollBar.width");
        return w > 0 ? w : 14; // standard Metal/Windows scrollbar width
    }

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        if (!(comp instanceof JScrollBar sb)) return;
        int w = sb.getWidth();
        int h = sb.getHeight();
        if (w <= 0 || h <= 0) return;

        Color trackBg = sb.getBackground() != null ? sb.getBackground()
                : uiColor("ScrollBar.background", new Color(0xE0E0E0));
        Color thumbFg = uiColor("ScrollBar.thumb", new Color(0x9E9E9E));

        // Track background
        ctx.writer().fillRect(absX, absY, w, h, trackBg);
        ctx.writer().strokeRect(absX, absY, w, h, trackBg.darker(), 0.5f);

        int min   = sb.getMinimum();
        int max   = sb.getMaximum();
        int vis   = sb.getVisibleAmount();
        int val   = sb.getValue();
        int range = max - min + vis; // total virtual extent
        if (range <= 0) return;

        if (sb.getOrientation() == SwingConstants.VERTICAL) {
            renderVertical(absX, absY, w, h, min, val, vis, range, trackBg, thumbFg, ctx);
        } else {
            renderHorizontal(absX, absY, w, h, min, val, vis, range, trackBg, thumbFg, ctx);
        }
    }

    // -----------------------------------------------------------------------

    private static void renderVertical(int absX, int absY, int w, int h,
                                        int min, int val, int vis, int range,
                                        Color trackBg, Color thumbFg,
                                        HandlerContext ctx) throws IOException {
        int arrowH = Math.min(ARROW_SIZE, h / 3);
        int trackH = h - 2 * arrowH;

        // End-cap buttons
        ctx.writer().fillRect(absX, absY,              w, arrowH, trackBg.darker());
        ctx.writer().fillRect(absX, absY + h - arrowH, w, arrowH, trackBg.darker());
        drawChevronV(absX, absY,              w, arrowH, true,  ctx);
        drawChevronV(absX, absY + h - arrowH, w, arrowH, false, ctx);

        // Thumb
        int thumbH = Math.max(8, (int)(trackH * (float) vis / range));
        int thumbY = absY + arrowH + (int)(trackH * (float)(val - min) / range);
        thumbY = Math.min(thumbY, absY + arrowH + trackH - thumbH);
        ctx.writer().fillRect(absX + 2, thumbY, w - 4, thumbH, thumbFg);
    }

    private static void renderHorizontal(int absX, int absY, int w, int h,
                                          int min, int val, int vis, int range,
                                          Color trackBg, Color thumbFg,
                                          HandlerContext ctx) throws IOException {
        int arrowW = Math.min(ARROW_SIZE, w / 3);
        int trackW = w - 2 * arrowW;

        ctx.writer().fillRect(absX,              absY, arrowW, h, trackBg.darker());
        ctx.writer().fillRect(absX + w - arrowW, absY, arrowW, h, trackBg.darker());
        drawChevronH(absX,              absY, arrowW, h, true,  ctx);
        drawChevronH(absX + w - arrowW, absY, arrowW, h, false, ctx);

        int thumbW = Math.max(8, (int)(trackW * (float) vis / range));
        int thumbX = absX + arrowW + (int)(trackW * (float)(val - min) / range);
        thumbX = Math.min(thumbX, absX + arrowW + trackW - thumbW);
        ctx.writer().fillRect(thumbX, absY + 2, thumbW, h - 4, thumbFg);
    }

    // -----------------------------------------------------------------------
    // Arrow chevrons (same two-line style as JComboBoxHandler)
    // -----------------------------------------------------------------------

    /** "^" or "v" chevron centred in the vertical end-cap button. */
    private static void drawChevronV(int x, int y, int w, int h, boolean up,
                                      HandlerContext ctx) throws IOException {
        int cx = x + w / 2;
        int cy = y + h / 2;
        int hw = 4, hh = 3;
        Color c = Color.DARK_GRAY;
        if (up) {
            ctx.writer().drawLine(cx - hw, cy + hh, cx,      cy - hh, c, 1.5f);
            ctx.writer().drawLine(cx,      cy - hh, cx + hw, cy + hh, c, 1.5f);
        } else {
            ctx.writer().drawLine(cx - hw, cy - hh, cx,      cy + hh, c, 1.5f);
            ctx.writer().drawLine(cx,      cy + hh, cx + hw, cy - hh, c, 1.5f);
        }
    }

    /** "<" or ">" chevron centred in the horizontal end-cap button. */
    private static void drawChevronH(int x, int y, int w, int h, boolean left,
                                      HandlerContext ctx) throws IOException {
        int cx = x + w / 2;
        int cy = y + h / 2;
        int hw = 3, hh = 4;
        Color c = Color.DARK_GRAY;
        if (left) {
            ctx.writer().drawLine(cx + hw, cy - hh, cx - hw, cy,      c, 1.5f);
            ctx.writer().drawLine(cx - hw, cy,      cx + hw, cy + hh, c, 1.5f);
        } else {
            ctx.writer().drawLine(cx - hw, cy - hh, cx + hw, cy,      c, 1.5f);
            ctx.writer().drawLine(cx + hw, cy,      cx - hw, cy + hh, c, 1.5f);
        }
    }

    private static Color uiColor(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }
}
