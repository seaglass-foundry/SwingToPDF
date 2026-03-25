package com.seaglassfoundry.swingtopdf.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.IOException;

import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * Renders a {@link JProgressBar} as PDF vector graphics.
 *
 * <p>Draws:
 * <ol>
 *   <li>Background fill</li>
 *   <li>Filled progress region (proportional to {@code value / (max - min)})</li>
 *   <li>Outer border</li>
 *   <li>String overlay when {@link JProgressBar#isStringPainted()} is {@code true}</li>
 * </ol>
 * Both {@link SwingConstants#HORIZONTAL} and {@link SwingConstants#VERTICAL}
 * orientations are supported.
 */
final class JProgressBarHandler implements ComponentHandler {

    static final JProgressBarHandler INSTANCE = new JProgressBarHandler();

    private JProgressBarHandler() {}

    @Override
    public void render(Component comp, int absX, int absY, HandlerContext ctx) throws IOException {
        JProgressBar bar = (JProgressBar) comp;
        int w = bar.getWidth();
        int h = bar.getHeight();

        // 1. Background
        Color bg = bar.getBackground() != null ? bar.getBackground() : Color.LIGHT_GRAY;
        ctx.writer().fillRect(absX, absY, w, h, bg);

        // 2. Progress fill
        float range    = bar.getMaximum() - bar.getMinimum();
        float fraction = range > 0
                ? Math.max(0f, Math.min(1f, (bar.getValue() - bar.getMinimum()) / range))
                : 0f;

        Color fillColor = bar.getForeground() != null ? bar.getForeground()
                : uiColor("ProgressBar.foreground", new Color(0, 128, 0));

        if (bar.getOrientation() == SwingConstants.HORIZONTAL) {
            int fillW = Math.round(w * fraction);
            if (fillW > 0) ctx.writer().fillRect(absX, absY, fillW, h, fillColor);
        } else {
            int fillH = Math.round(h * fraction);
            if (fillH > 0) ctx.writer().fillRect(absX, absY + h - fillH, w, fillH, fillColor);
        }

        // 3. Border
        ctx.writer().strokeRect(absX, absY, w, h, Color.GRAY, 1f);

        // 4. String overlay
        if (bar.isStringPainted()) {
            String text = bar.getString();
            if (text == null) text = Math.round(fraction * 100) + "%";
            if (!text.isBlank()) {
                Font font = bar.getFont() != null ? bar.getFont()
                                                  : new Font(Font.SANS_SERIF, Font.PLAIN, 11);
                Color fg = contrastColor(fillColor, fraction, w, h,
                                         bar.getOrientation() == SwingConstants.HORIZONTAL);
                FontMetrics fm     = bar.getFontMetrics(font);
                int         textW  = fm.stringWidth(text);
                int         ascent = fm.getAscent();
                int         textH  = ascent + fm.getDescent();
                int         textX  = absX + (w - textW) / 2;
                int         baseY  = absY + (h - textH) / 2 + ascent;
                ctx.writer().drawText(text, ctx.fontMapper().resolve(font),
                                      font.getSize2D(), fg, textX, baseY);
            }
        }
    }

    // -----------------------------------------------------------------------

    /**
     * Pick black or white text so it contrasts with whatever colour lies
     * beneath the text centre.
     */
    private static Color contrastColor(Color fillColor, float fraction,
                                        int w, int h, boolean horizontal) {
        // Is the centre of the bar covered by the fill?
        boolean centreInFill = fraction >= 0.5f;
        Color under = centreInFill ? fillColor : Color.LIGHT_GRAY;
        double lum = 0.299 * under.getRed() + 0.587 * under.getGreen() + 0.114 * under.getBlue();
        return lum < 128 ? Color.WHITE : Color.BLACK;
    }

    private static Color uiColor(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }
}
